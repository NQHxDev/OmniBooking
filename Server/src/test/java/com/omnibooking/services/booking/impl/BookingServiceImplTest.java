package com.omnibooking.services.booking.impl;

import com.omnibooking.dto.BookingResponse;
import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.model.Booking;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.Role;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.BookingRepository;
import com.omnibooking.repository.BookingStatusLogRepository;
import com.omnibooking.repository.CouponRepository;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.RoomAvailabilityRepository;
import com.omnibooking.repository.RoomTypeRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.services.core.CurrencyService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.user.VerificationService;
import com.omnibooking.repository.TransactionRepository;
import com.omnibooking.model.Transaction;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.services.auth.CachedRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl Unit Tests for Deposits")
class BookingServiceImplTest {

   @Mock
   private BookingRepository bookingRepository;
   @Mock
   private RoomTypeRepository roomTypeRepository;
   @Mock
   private RoomAvailabilityRepository roomAvailabilityRepository;
   @Mock
   private UserRepository userRepository;
   @Mock
   private UserProfileRepository userProfileRepository;
   @Mock
   private RoleRepository roleRepository;
   @Mock
   private CachedRoleService cachedRoleService;
   @Mock
   private CouponRepository couponRepository;
   @Mock
   private BookingStatusLogRepository bookingStatusLogRepository;
   @Mock
   private TransactionRepository transactionRepository;

   @Mock
   private EncryptionService encryptionService;
   @Mock
   private BloomFilterService bloomFilterService;
   @Mock
   private VerificationService verificationService;
   @Mock
   private MailService mailService;
   @Mock
   private OutboxService outboxService;
   @Mock
   private CurrencyService currencyService;
   @Mock
   private PasswordEncoder passwordEncoder;
   @Mock
   private CacheManager cacheManager;
   @Mock
   private Cache cache;

   @InjectMocks
   private BookingServiceImpl bookingService;

   private RoomType mockRoomType;
   private User mockOwner;
   private Property mockProperty;
   private Role mockRole;

   @BeforeEach
   void setUp() {
      mockOwner = User.builder()
            .id(UUID.randomUUID())
            .email("partner@example.com")
            .build();

      mockProperty = Property.builder()
            .id(UUID.randomUUID())
            .name("Grand Palace")
            .owner(mockOwner)
            .propertyType(PropertyType.HOTEL)
            .address("123 Street")
            .city("Hanoi")
            .country("Vietnam")
            .build();

      mockRoomType = RoomType.builder()
            .id(UUID.randomUUID())
            .name("Deluxe Room")
            .basePrice(new BigDecimal("100.00"))
            .totalRooms(10)
            .property(mockProperty)
            .build();

      mockRole = Role.builder()
            .id(UUID.randomUUID())
            .name("ROLE_USER")
            .build();

      lenient().when(roomTypeRepository.findById(mockRoomType.getId())).thenReturn(Optional.of(mockRoomType));

      lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
         Booking b = invocation.getArgument(0);
         if (b.getId() == null) {
            b.setId(UUID.randomUUID());
         }
         return b;
      });

      lenient().when(currencyService.convertFromBase(any(BigDecimal.class), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));

      lenient().when(mailService.buildBookingConfirmationEmailEvent(
            anyString(), anyString(), any(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(new EmailEvent());

      lenient().when(cacheManager.getCache("partner_bookings")).thenReturn(cache);
   }

   private void mockRoomAvailability(LocalDate checkIn, LocalDate checkOut, BigDecimal price) {
      LocalDate date = checkIn;
      while (date.isBefore(checkOut)) {
         RoomAvailability availability = RoomAvailability.builder()
               .roomType(mockRoomType)
               .availabilityDate(date)
               .availableCount(mockRoomType.getTotalRooms())
               .priceOverride(price)
               .isClosed(false)
               .build();
         lenient()
               .when(roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDateWithLock(mockRoomType.getId(), date))
               .thenReturn(Optional.of(availability));
         date = date.plusDays(1);
      }
   }

   @Test
   @DisplayName("Guest (Unregistered) Booking: Should always require deposit and cap it at 1 night")
   void testGuestBooking_RequiresDeposit_CappedAtOneNight() {
      // Arrange: guest booking 3 nights, check-in is 2 days away.
      // Total price: 3 nights * 100 USD = 300 USD.
      // 15% is 45 USD. Capped at 1 night = 100 USD.
      // Calculated deposit amount should be 45 USD.
      LocalDate checkIn = LocalDate.now().plusDays(2);
      LocalDate checkOut = checkIn.plusDays(3);
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(mockRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("John Doe")
            .guestEmail("john@example.com")
            .currency("USD")
            .build();

      mockRoomAvailability(checkIn, checkOut, null);
      when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
      when(cachedRoleService.getRoleByName("ROLE_USER")).thenReturn(mockRole);
      when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
         User u = invocation.getArgument(0);
         u.setId(UUID.randomUUID());
         return u;
      });
      when(verificationService.createVerificationToken(any(UUID.class))).thenReturn("verify_token");

      // Act
      BookingResponse response = bookingService.createBooking(request, null);

      // Assert
      assertThat(response.getRequiresDeposit()).isTrue();
      assertThat(response.getDepositAmount().setScale(2, RoundingMode.HALF_UP))
            .isEqualTo(new BigDecimal("45.00"));
      assertThat(response.getTotalPrice().setScale(2, RoundingMode.HALF_UP))
            .isEqualTo(new BigDecimal("300.00"));
   }

   @Test
   @DisplayName("Guest (Unregistered) Booking: Should cap deposit to 1 night price if 15% is larger")
   void testGuestBooking_RequiresDeposit_CappedToExactlyOneNightPrice() {
      // Arrange: guest booking 10 nights.
      // Total price: 10 nights * 100 USD = 1000 USD.
      // 15% is 150 USD. Capped at 1 night = 100 USD.
      // Calculated deposit amount should be 100 USD.
      LocalDate checkIn = LocalDate.now().plusDays(2);
      LocalDate checkOut = checkIn.plusDays(10);
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(mockRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("John Doe")
            .guestEmail("john@example.com")
            .currency("USD")
            .build();

      mockRoomAvailability(checkIn, checkOut, null);
      when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
      when(cachedRoleService.getRoleByName("ROLE_USER")).thenReturn(mockRole);
      when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
         User u = invocation.getArgument(0);
         u.setId(UUID.randomUUID());
         return u;
      });
      when(verificationService.createVerificationToken(any(UUID.class))).thenReturn("verify_token");

      // Act
      BookingResponse response = bookingService.createBooking(request, null);

      // Assert
      assertThat(response.getRequiresDeposit()).isTrue();
      assertThat(response.getDepositAmount().setScale(2, RoundingMode.HALF_UP))
            .isEqualTo(new BigDecimal("100.00"));
   }

   @Test
   @DisplayName("Registered Booking (Check-in < 5 days): Should NOT require deposit")
   void testRegisteredBooking_CheckInLessThan5Days_DoesNotRequireDeposit() {
      // Arrange: check-in is 4 days away.
      LocalDate checkIn = LocalDate.now().plusDays(4);
      LocalDate checkOut = checkIn.plusDays(3);
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(mockRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("Jane Doe")
            .guestEmail("jane@example.com")
            .currency("USD")
            .build();

      User mockUser = User.builder()
            .id(UUID.randomUUID())
            .email("jane@example.com")
            .username("jane_doe")
            .isActive(true)
            .roles(Collections.singleton(mockRole))
            .build();

      UserPrincipal principal = UserPrincipal.create(mockUser);

      mockRoomAvailability(checkIn, checkOut, null);
      when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));

      // Act
      BookingResponse response = bookingService.createBooking(request, principal);

      // Assert
      assertThat(response.getRequiresDeposit()).isFalse();
      assertThat(response.getDepositAmount().compareTo(BigDecimal.ZERO)).isEqualTo(0);
   }

   @Test
   @DisplayName("Registered Booking (Check-in >= 5 days): Should require deposit")
   void testRegisteredBooking_CheckIn5DaysOrMore_RequiresDeposit() {
      // Arrange: check-in is exactly 5 days away.
      // Total price: 3 nights * 100 USD = 300 USD.
      // 15% is 45 USD. Capped at 1 night = 100 USD.
      LocalDate checkIn = LocalDate.now().plusDays(5);
      LocalDate checkOut = checkIn.plusDays(3);
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(mockRoomType.getId())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numRooms(1)
            .guestName("Jane Doe")
            .guestEmail("jane@example.com")
            .currency("USD")
            .build();

      User mockUser = User.builder()
            .id(UUID.randomUUID())
            .email("jane@example.com")
            .username("jane_doe")
            .isActive(true)
            .roles(Collections.singleton(mockRole))
            .build();

      UserPrincipal principal = UserPrincipal.create(mockUser);

      mockRoomAvailability(checkIn, checkOut, null);
      when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));

      // Act
      BookingResponse response = bookingService.createBooking(request, principal);

      // Assert
      assertThat(response.getRequiresDeposit()).isTrue();
      assertThat(response.getDepositAmount().setScale(2, RoundingMode.HALF_UP))
            .isEqualTo(new BigDecimal("45.00"));
   }

   @Test
   @DisplayName("confirmBooking: Should update booking status to CONFIRMED and save Transaction")
   void testConfirmBooking_Success() {
      // Arrange
      UUID bookingId = UUID.randomUUID();
      Booking mockBooking = Booking.builder()
            .id(bookingId)
            .status(BookingStatus.PENDING)
            .depositAmount(new BigDecimal("45.00"))
            .guestEmail("john@example.com")
            .guestName("John Doe")
            .currency("USD")
            .totalPrice(new BigDecimal("300.00"))
            .finalPrice(new BigDecimal("300.00"))
            .roomType(mockRoomType)
            .checkInDate(LocalDate.now().plusDays(2))
            .checkOutDate(LocalDate.now().plusDays(5))
            .build();

      when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
      when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      bookingService.confirmBooking(bookingId, "MOMO", "momo_trans_123", "{}");

      // Assert
      assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
      verify(bookingRepository, times(1)).save(mockBooking);
      verify(bookingStatusLogRepository, times(1)).save(any());
      verify(transactionRepository, times(1)).save(any(Transaction.class));
      verify(outboxService, times(1)).saveEvent(eq(bookingId), eq("BOOKING"), eq("BOOKING_CONFIRMED_MAIL"), any());
   }

   @Test
   @DisplayName("getBookingById: Should return booking details correctly")
   void testGetBookingById_Success() {
      // Arrange
      UUID bookingId = UUID.randomUUID();
      Booking mockBooking = Booking.builder()
            .id(bookingId)
            .status(BookingStatus.CONFIRMED)
            .depositAmount(new BigDecimal("45.00"))
            .guestEmail("john@example.com")
            .guestName("John Doe")
            .currency("USD")
            .totalPrice(new BigDecimal("300.00"))
            .finalPrice(new BigDecimal("300.00"))
            .roomType(mockRoomType)
            .checkInDate(LocalDate.now().plusDays(2))
            .checkOutDate(LocalDate.now().plusDays(5))
            .numRooms(1)
            .requiresDeposit(true)
            .paymentMethod("MOMO")
            .build();

      when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

      // Act
      BookingResponse response = bookingService.getBookingById(bookingId);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(bookingId);
      assertThat(response.getBookingCode()).isEqualTo(bookingId.toString().substring(0, 8).toUpperCase());
      assertThat(response.getGuestName()).isEqualTo("John Doe");
      assertThat(response.getPropertyName()).isEqualTo("Grand Palace");
      assertThat(response.getRoomTypeName()).isEqualTo("Deluxe Room");
      assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
   }
}
