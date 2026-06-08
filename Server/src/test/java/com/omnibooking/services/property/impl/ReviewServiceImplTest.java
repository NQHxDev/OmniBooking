package com.omnibooking.services.property.impl;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.dto.CreateReviewRequest;
import com.omnibooking.dto.PageResponse;
import com.omnibooking.dto.ReviewReplyRequest;
import com.omnibooking.dto.ReviewResponse;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Booking;
import com.omnibooking.model.Property;
import com.omnibooking.model.Review;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.ReviewStatus;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.services.core.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl Unit Tests")
class ReviewServiceImplTest {

   @Mock
   private ReviewRepository reviewRepository;

   @Mock
   private PropertyRepository propertyRepository;

   @Mock
   private BookingRepository bookingRepository;

   @Mock
   private UserRepository userRepository;

   @Mock
   private OutboxService outboxService;

   @Mock
   private StringRedisTemplate redisTemplate;

   @Mock
   private MeterRegistry meterRegistry;

   @Mock
   private ZSetOperations<String, String> zSetOperations;

   @Mock
   private Counter counter;

   @Mock
   private Timer timer;

   @InjectMocks
   private ReviewServiceImpl reviewService;

   private UUID userId;

   private UUID propertyId;

   private UUID bookingId;

   private User user;

   private Property property;

   private Booking booking;

   @BeforeEach
   void setUp() {
      userId = UUID.randomUUID();
      propertyId = UUID.randomUUID();
      bookingId = UUID.randomUUID();

      User owner = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .build();

      user = User.builder()
            .id(userId)
            .email("guest@example.com")
            .username("guest")
            .profile(UserProfile.builder().displayName("John Doe").avatarUrl("avatar.jpg").build())
            .build();

      property = Property.builder()
            .id(propertyId)
            .name("Test Hotel")
            .owner(owner)
            .averageRating(BigDecimal.ZERO)
            .reviewCount(0)
            .ratingSum(0L)
            .build();

      booking = Booking.builder()
            .id(bookingId)
            .user(user)
            .roomType(com.omnibooking.model.RoomType.builder().property(property).build())
            .status(BookingStatus.STAYED)
            .checkInDate(LocalDate.now().minusDays(5))
            .checkOutDate(LocalDate.now().minusDays(1))
            .build();

      // Lenient mocking for meter registry
      lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
      lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
      lenient().when(meterRegistry.timer(anyString())).thenReturn(timer);
      lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
   }

   @Nested
   @DisplayName("Create Review Tests")
   class CreateReviewTests {

      @Test
      @DisplayName("Should successfully create review when valid")
      void createReview_Success() {
         // Arrange
         CreateReviewRequest request = CreateReviewRequest.builder()
               .bookingId(bookingId)
               .rating(5)
               .comment("Excellent stay here!")
               .build();

         when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);
         when(zSetOperations.zCard(anyString())).thenReturn(0L);
         when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
         when(reviewRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
         when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
         when(userRepository.findById(userId)).thenReturn(Optional.of(user));

         Review savedReview = Review.builder()
               .id(UUID.randomUUID())
               .booking(booking)
               .property(property)
               .user(user)
               .rating(request.getRating())
               .comment(request.getComment())
               .status(ReviewStatus.PUBLISHED)
               .createdAt(Instant.now())
               .updatedAt(Instant.now())
               .build();

         when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

         // Act
         ReviewResponse response = reviewService.createReview(userId, request);

         // Assert
         assertThat(response).isNotNull();
         assertThat(response.getRating()).isEqualTo(5);
         assertThat(response.getComment()).isEqualTo("Excellent stay here!");
         assertThat(response.getPropertyName()).isEqualTo("Test Hotel");
         assertThat(response.getUserName()).isEqualTo("John Doe");

         // Verify atomic increment was called
         verify(propertyRepository).incrementRating(propertyId, 5);
         verify(outboxService).saveEvent(eq(propertyId), eq("PROPERTY"), eq(EventConstants.PROPERTY_SYNC), any());
      }

      @Test
      @DisplayName("Should throw RATE_LIMIT_EXCEEDED when user writes too many reviews")
      void createReview_RateLimitExceeded() {
         // Arrange
         CreateReviewRequest request = CreateReviewRequest.builder()
               .bookingId(bookingId)
               .rating(5)
               .build();

         when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);
         when(zSetOperations.zCard(anyString())).thenReturn(5L);

         // Act & Assert
         assertThatThrownBy(() -> reviewService.createReview(userId, request))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.RATE_LIMIT_EXCEEDED);

         verify(bookingRepository, never()).findById(any());
      }

      @Test
      @DisplayName("Should throw CHRONOLOGICAL_VALIDATION_FAILED when review is before checkOutDate")
      void createReview_BeforeCheckout() {
         // Arrange
         CreateReviewRequest request = CreateReviewRequest.builder()
               .bookingId(bookingId)
               .rating(5)
               .build();

         booking.setCheckOutDate(LocalDate.now().plusDays(2)); // Checkout in future

         when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);
         when(zSetOperations.zCard(anyString())).thenReturn(0L);
         when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

         // Act & Assert
         assertThatThrownBy(() -> reviewService.createReview(userId, request))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.CHRONOLOGICAL_VALIDATION_FAILED);
      }

      @Test
      @DisplayName("Should throw MINIMUM_TEXT_LENGTH_VIOLATION when comment is less than 10 characters")
      void createReview_CommentTooShort() {
         // Arrange
         CreateReviewRequest request = CreateReviewRequest.builder()
               .bookingId(bookingId)
               .rating(5)
               .comment("short")
               .build();

         when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);
         when(zSetOperations.zCard(anyString())).thenReturn(0L);
         when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
         when(reviewRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

         // Act & Assert
         assertThatThrownBy(() -> reviewService.createReview(userId, request))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.MINIMUM_TEXT_LENGTH_VIOLATION);
      }
   }

   @Nested
   @DisplayName("Partner Reply Tests")
   class PartnerReplyTests {

      @Test
      @DisplayName("Should successfully reply to review when owner")
      void replyToReview_Success() {
         // Arrange
         UUID reviewId = UUID.randomUUID();
         ReviewReplyRequest request = ReviewReplyRequest.builder()
               .reply("Thank you for your warm words!")
               .build();

         Review review = Review.builder()
               .id(reviewId)
               .property(property)
               .booking(booking)
               .rating(5)
               .status(ReviewStatus.PUBLISHED)
               .build();

         when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
         when(reviewRepository.save(any(Review.class))).thenReturn(review);

         // Act
         ReviewResponse response = reviewService.replyToReview(property.getOwner().getId(), reviewId, request);

         // Assert
         assertThat(response).isNotNull();
         assertThat(response.getReply()).isEqualTo("Thank you for your warm words!");
         verify(reviewRepository).save(review);
      }

      @Test
      @DisplayName("Should throw NOT_PROPERTY_OWNER when replier is not owner")
      void replyToReview_NotOwner() {
         // Arrange
         UUID reviewId = UUID.randomUUID();
         ReviewReplyRequest request = ReviewReplyRequest.builder()
               .reply("Thank you!")
               .build();

         Review review = Review.builder()
               .id(reviewId)
               .property(property)
               .booking(booking)
               .rating(5)
               .build();

         when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

         // Act & Assert
         assertThatThrownBy(() -> reviewService.replyToReview(UUID.randomUUID(), reviewId, request))
               .isInstanceOf(AppException.class)
               .hasFieldOrPropertyWithValue("errorEnum", ErrorCode.NOT_PROPERTY_OWNER);
      }
   }

   @Nested
   @DisplayName("Soft Delete Tests")
   class SoftDeleteTests {

      @Test
      @DisplayName("Should soft delete and update property aggregates")
      void deleteReview_Success() {
         // Arrange
         UUID reviewId = UUID.randomUUID();
         Review review = Review.builder()
               .id(reviewId)
               .property(property)
               .booking(booking)
               .rating(5)
               .status(ReviewStatus.PUBLISHED)
               .build();

         when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
         when(userRepository.findById(userId)).thenReturn(Optional.of(user));

         // Act
         reviewService.deleteReview(userId, reviewId, "Spam comment");

         // Assert
         assertThat(review.getStatus()).isEqualTo(ReviewStatus.REMOVED);
         assertThat(review.getDeletedAt()).isNotNull();
         assertThat(review.getDeletedBy()).isEqualTo(user);
         assertThat(review.getDeletionReason()).isEqualTo("Spam comment");

         // Verify atomic decrement was called
         verify(propertyRepository).decrementRating(propertyId, 5);
         verify(reviewRepository).save(review);
         verify(outboxService).saveEvent(eq(propertyId), eq("PROPERTY"), eq(EventConstants.PROPERTY_SYNC), any());
      }
   }

   @Nested
   @DisplayName("Moderation Tests")
   class ModerationTests {

      @Test
      @DisplayName("Should hide review and decrease property ratings")
      void hideReview_Success() {
         // Arrange
         UUID reviewId = UUID.randomUUID();
         Review review = Review.builder()
               .id(reviewId)
               .property(property)
               .booking(booking)
               .rating(4)
               .status(ReviewStatus.PUBLISHED)
               .build();

         UUID adminId = UUID.randomUUID();
         User admin = User.builder().id(adminId).username("admin").build();

         when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
         when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
         when(reviewRepository.save(any(Review.class))).thenReturn(review);

         // Act
         ReviewResponse response = reviewService.hideReview(adminId, reviewId, "Inappropriate text");

         // Assert
         assertThat(response.getStatus()).isEqualTo("HIDDEN");
         assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
         assertThat(review.getModeratedAt()).isNotNull();
         assertThat(review.getModeratedBy()).isEqualTo(admin);
         assertThat(review.getModerationReason()).isEqualTo("Inappropriate text");

         // Verify atomic decrement was called
         verify(propertyRepository).decrementRating(propertyId, 4);
      }

      @Test
      @DisplayName("Should restore hidden review and increase property ratings")
      void restoreReview_Success() {
         // Arrange
         UUID reviewId = UUID.randomUUID();
         Review review = Review.builder()
               .id(reviewId)
               .property(property)
               .booking(booking)
               .rating(4)
               .status(ReviewStatus.HIDDEN)
               .build();

         UUID adminId = UUID.randomUUID();
         User admin = User.builder().id(adminId).username("admin").build();

         when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
         when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
         when(reviewRepository.save(any(Review.class))).thenReturn(review);

         // Act
         ReviewResponse response = reviewService.restoreReview(adminId, reviewId);

         // Assert
         assertThat(response.getStatus()).isEqualTo("PUBLISHED");
         assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
         assertThat(review.getDeletedAt()).isNull();

         // Verify atomic increment was called
         verify(propertyRepository).incrementRating(propertyId, 4);
      }
   }

   @Nested
   @DisplayName("GDPR & Null User Mapping Tests")
   class GdprTests {

      @Test
      @DisplayName("Should map review successfully and default username to Anonymous when user is null")
      void getPropertyReviews_NullUser_GdprCompliance() {
         // Arrange
         Pageable pageable = PageRequest.of(0, 10);
         Review review = Review.builder()
               .id(UUID.randomUUID())
               .property(property)
               .booking(booking)
               .rating(4)
               .user(null) // Anonymized user (account deleted)
               .comment("Great experience.")
               .status(ReviewStatus.PUBLISHED)
               .build();

         Page<Review> reviewPage = new PageImpl<>(Collections.singletonList(review), pageable, 1);

         when(reviewRepository.findByPropertyIdAndStatusAndDeletedAtIsNull(eq(propertyId), eq(ReviewStatus.PUBLISHED),
               any(Pageable.class)))
               .thenReturn(reviewPage);

         // Act
         PageResponse<ReviewResponse> response = reviewService.getPropertyReviews(propertyId, pageable);

         // Assert
         assertThat(response).isNotNull();
         assertThat(response.getItems()).hasSize(1);
         ReviewResponse mapped = response.getItems().get(0);
         assertThat(mapped.getUserName()).isEqualTo("Anonymous");
         assertThat(mapped.getUserAvatarUrl()).isNull();
         assertThat(mapped.getUserId()).isNull();
      }
   }

   @Nested
   @DisplayName("Query Tests")
   class QueryTests {

      @Test
      @DisplayName("Should fetch property reviews with clamping page size")
      void getPropertyReviews_Success() {
         // Arrange
         Pageable pageable = PageRequest.of(0, 5); // size under 10 will clamp to 10
         Page<Review> reviewPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

         when(reviewRepository.findByPropertyIdAndStatusAndDeletedAtIsNull(eq(propertyId), eq(ReviewStatus.PUBLISHED),
               any(Pageable.class)))
               .thenReturn(reviewPage);

         // Act
         PageResponse<ReviewResponse> response = reviewService.getPropertyReviews(propertyId, pageable);

         // Assert
         assertThat(response).isNotNull();
         assertThat(response.getItems()).isEmpty();
         // Verify clamped page size was used
         verify(reviewRepository).findByPropertyIdAndStatusAndDeletedAtIsNull(eq(propertyId),
               eq(ReviewStatus.PUBLISHED), eq(PageRequest.of(0, 10)));
      }
   }
}
