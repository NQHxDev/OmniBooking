package com.omnibooking.services.booking.impl;

import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.dto.BookingResponse;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Booking;
import com.omnibooking.model.BookingStatusLog;
import com.omnibooking.model.Coupon;
import com.omnibooking.model.Role;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.DiscountType;
import com.omnibooking.repository.BookingRepository;
import com.omnibooking.repository.BookingStatusLogRepository;
import com.omnibooking.repository.CouponRepository;
import com.omnibooking.repository.RoleRepository;
import com.omnibooking.repository.RoomAvailabilityRepository;
import com.omnibooking.repository.RoomTypeRepository;
import com.omnibooking.repository.UserRepository;
import com.omnibooking.repository.UserProfileRepository;
import com.omnibooking.repository.TransactionRepository;
import com.omnibooking.model.Transaction;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.booking.BookingService;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.services.core.CurrencyService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.user.VerificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

   private final BookingRepository bookingRepository;
   private final RoomTypeRepository roomTypeRepository;
   private final RoomAvailabilityRepository roomAvailabilityRepository;
   private final UserRepository userRepository;
   private final UserProfileRepository userProfileRepository;
   private final RoleRepository roleRepository;
   private final CouponRepository couponRepository;
   private final BookingStatusLogRepository bookingStatusLogRepository;
   private final TransactionRepository transactionRepository;

   private final EncryptionService encryptionService;
   private final BloomFilterService bloomFilterService;
   private final VerificationService verificationService;
   private final MailService mailService;
   private final OutboxService outboxService;
   private final CurrencyService currencyService;
   private final PasswordEncoder passwordEncoder;
   private final org.springframework.cache.CacheManager cacheManager;

   @Override
   @Transactional
   public BookingResponse createBooking(CreateBookingRequest request, UserPrincipal principal) {
      log.info("Creating booking for email: {}, RoomType: {}", request.getGuestEmail(), request.getRoomTypeId());

      if (request.getCheckInDate().isAfter(request.getCheckOutDate())
            || request.getCheckInDate().isEqual(request.getCheckOutDate())) {
         throw new AppException("BOOKING_002", "Check-in date must be before check-out date", HttpStatus.BAD_REQUEST);
      }

      RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found"));

      // 1. Resolve User (Logged in vs. Guest)
      User user = null;
      boolean isNewGuest = false;
      boolean isInactiveGuest = false;

      if (principal != null) {
         user = userRepository.findById(principal.getId())
               .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
      } else {
         String email = request.getGuestEmail().trim().toLowerCase();
         Optional<User> existingUserOpt = userRepository.findByEmail(email);
         if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (Boolean.FALSE.equals(user.getIsActive())) {
               isInactiveGuest = true;
            }
         } else {
            isNewGuest = true;
            Role userRole = roleRepository.findByName("ROLE_USER")
                  .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            String username = "guest_" + UUID.randomUUID().toString().substring(0, 8);
            String rawPassword = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(rawPassword);

            user = User.builder()
                  .username(username)
                  .email(email)
                  .password(encodedPassword)
                  .isActive(false)
                  .roles(Collections.singleton(userRole))
                  .build();
            user = userRepository.save(user);

            bloomFilterService.add(email);

            UserProfile profile = UserProfile.builder()
                  .user(user)
                  .displayName(request.getGuestName())
                  .phoneEncrypted(
                        request.getGuestPhone() != null ? encryptionService.encrypt(request.getGuestPhone()) : null)
                  .phoneSearchHash(
                        request.getGuestPhone() != null ? encryptionService.createBlindIndex(request.getGuestPhone())
                              : null)
                  .build();
            userProfileRepository.save(profile);
         }
      }

      // 2. Room Availability Check & Locks (Pessimistic write locks for each day)
      BigDecimal basePriceSum = BigDecimal.ZERO;
      LocalDate date = request.getCheckInDate();
      while (date.isBefore(request.getCheckOutDate())) {
         final LocalDate finalDate = date;
         RoomAvailability availability = roomAvailabilityRepository
               .findByRoomTypeIdAndAvailabilityDateWithLock(roomType.getId(), date)
               .orElseGet(() -> {
                  // Auto-initialize if not pre-populated
                  RoomAvailability init = RoomAvailability.builder()
                        .roomType(roomType)
                        .availabilityDate(finalDate)
                        .availableCount(roomType.getTotalRooms())
                        .isClosed(false)
                        .build();
                  return roomAvailabilityRepository.save(init);
               });

         if (Boolean.TRUE.equals(availability.getIsClosed())
               || availability.getAvailableCount() < request.getNumRooms()) {
            throw new AppException("BOOKING_001", "Room not available for date: " + date, HttpStatus.BAD_REQUEST);
         }

         // Deduct availability
         availability.setAvailableCount(availability.getAvailableCount() - request.getNumRooms());
         roomAvailabilityRepository.save(availability);

         // Price Override check
         BigDecimal dayPrice = availability.getPriceOverride() != null ? availability.getPriceOverride()
               : roomType.getBasePrice();
         basePriceSum = basePriceSum.add(dayPrice);

         date = date.plusDays(1);
      }

      BigDecimal totalPrice = basePriceSum.multiply(BigDecimal.valueOf(request.getNumRooms()));
      BigDecimal finalPrice = totalPrice;

      // 3. Coupon processing
      Coupon coupon = null;
      if (request.getCouponId() != null) {
         coupon = couponRepository.findById(request.getCouponId())
               .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Coupon not found"));

         if (Boolean.FALSE.equals(coupon.getIsActive()) ||
               coupon.getValidFrom().isAfter(Instant.now()) ||
               coupon.getValidUntil().isBefore(Instant.now())) {
            throw new AppException("BOOKING_003", "Coupon is inactive or expired", HttpStatus.BAD_REQUEST);
         }

         if (totalPrice.compareTo(coupon.getMinBookingAmount()) < 0) {
            throw new AppException("BOOKING_004", "Minimum booking amount for coupon not met", HttpStatus.BAD_REQUEST);
         }

         if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new AppException("BOOKING_005", "Coupon usage limit reached", HttpStatus.BAD_REQUEST);
         }

         BigDecimal discount = BigDecimal.ZERO;
         if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discount = totalPrice.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
         } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = coupon.getDiscountValue();
         }

         if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
         }

         finalPrice = totalPrice.subtract(discount);
         if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
         }

         coupon.setUsedCount(coupon.getUsedCount() + 1);
         couponRepository.save(coupon);
      }

      // 4. Calculate Deposit Requirements
      boolean requiresDeposit = false;
      if (principal == null) {
         requiresDeposit = true;
      } else {
         long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), request.getCheckInDate());
         if (daysBetween >= 5) {
            requiresDeposit = true;
         }
      }

      BigDecimal depositAmount = BigDecimal.ZERO;
      if (requiresDeposit) {
         BigDecimal fifteenPercent = finalPrice.multiply(new BigDecimal("0.15"));
         BigDecimal firstNightRoomPrice = roomType.getBasePrice();
         Optional<RoomAvailability> firstDayOpt = roomAvailabilityRepository
               .findByRoomTypeIdAndAvailabilityDateWithLock(roomType.getId(), request.getCheckInDate());
         if (firstDayOpt.isPresent() && firstDayOpt.get().getPriceOverride() != null) {
            firstNightRoomPrice = firstDayOpt.get().getPriceOverride();
         }
         BigDecimal oneNightTotal = firstNightRoomPrice.multiply(BigDecimal.valueOf(request.getNumRooms()));
         depositAmount = fifteenPercent.min(oneNightTotal).setScale(4, RoundingMode.HALF_UP);
      }

      // 4. Save Booking
      boolean isPendingMomo = requiresDeposit && "momo".equalsIgnoreCase(request.getPaymentMethod());
      BookingStatus initialStatus = isPendingMomo ? BookingStatus.PENDING : BookingStatus.CONFIRMED;

      Booking booking = Booking.builder()
            .user(user)
            .roomType(roomType)
            .checkInDate(request.getCheckInDate())
            .checkOutDate(request.getCheckOutDate())
            .numRooms(request.getNumRooms())
            .totalPrice(totalPrice)
            .finalPrice(finalPrice)
            .currency(request.getCurrency())
            .coupon(coupon)
            .status(initialStatus)
            .guestName(request.getGuestName())
            .guestEmail(request.getGuestEmail())
            .guestPhoneEncrypted(
                  request.getGuestPhone() != null ? encryptionService.encrypt(request.getGuestPhone()) : null)
            .guestPhoneSearchHash(
                  request.getGuestPhone() != null ? encryptionService.createBlindIndex(request.getGuestPhone()) : null)
            .specialRequests(request.getSpecialRequests())
            .paymentMethod(request.getPaymentMethod())
            .requiresDeposit(requiresDeposit)
            .depositAmount(depositAmount)
            .build();

      booking = bookingRepository.save(booking);

      // Create Booking Status Log
      BookingStatusLog logEntry = BookingStatusLog.builder()
            .booking(booking)
            .oldStatus(null)
            .newStatus(initialStatus)
            .reason(isPendingMomo ? "Initial booking created, pending MoMo deposit payment" : "Initial booking created and confirmed")
            .changedBy(user)
            .build();
      bookingStatusLogRepository.save(logEntry);

      // 5. Send outbox confirmation email & activation token if guest is new or inactive
      String token = null;
      if (isNewGuest || isInactiveGuest) {
         token = verificationService.createVerificationToken(user.getId());
      }

      String bookingCode = booking.getId().toString().substring(0, 8).toUpperCase();

      if (!isPendingMomo) {
         String bookingCurrency = booking.getCurrency();
         BigDecimal convertedTotal = currencyService.convertFromBase(totalPrice, bookingCurrency);
         BigDecimal convertedFinal = currencyService.convertFromBase(finalPrice, bookingCurrency);

         String formattedTotal = formatCurrency(convertedTotal, bookingCurrency);
         String formattedFinal = formatCurrency(convertedFinal, bookingCurrency);

         String secondaryTotal = null;
         String secondaryFinal = null;
         if ("VND".equalsIgnoreCase(bookingCurrency)) {
            secondaryTotal = "(" + formatCurrency(totalPrice, "USD") + ")";
            secondaryFinal = "(" + formatCurrency(finalPrice, "USD") + ")";
         }

         EmailEvent emailEvent = mailService.buildBookingConfirmationEmailEvent(
               booking.getGuestEmail(),
               booking.getGuestName(),
               token,
               bookingCode,
               roomType.getProperty().getName(),
               roomType.getName(),
               booking.getCheckInDate().toString(),
               booking.getCheckOutDate().toString(),
               formattedTotal,
               formattedFinal,
               secondaryTotal,
               secondaryFinal);

         outboxService.saveEvent(
               booking.getId(),
               "BOOKING",
               "BOOKING_CONFIRMED_MAIL",
               emailEvent);

         // Evict partner bookings list cache to show the new booking on their management page
         try {
            org.springframework.cache.Cache cache = cacheManager.getCache("partner_bookings");
            if (cache != null) {
               cache.evict(roomType.getProperty().getOwner().getId());
               log.info("Evicted partner_bookings cache for partner: {}", roomType.getProperty().getOwner().getId());
            }
         } catch (Exception e) {
            log.error("Failed to evict partner_bookings cache", e);
         }
      }

      return BookingResponse.builder()
            .id(booking.getId())
            .bookingCode(bookingCode)
            .guestName(booking.getGuestName())
            .guestEmail(booking.getGuestEmail())
            .propertyName(roomType.getProperty().getName())
            .roomTypeName(roomType.getName())
            .checkInDate(booking.getCheckInDate())
            .checkOutDate(booking.getCheckOutDate())
            .numRooms(booking.getNumRooms())
            .totalPrice(totalPrice)
            .finalPrice(finalPrice)
            .status(booking.getStatus())
            .activationToken(token)
            .currency(booking.getCurrency())
            .depositAmount(booking.getDepositAmount())
            .requiresDeposit(booking.getRequiresDeposit())
            .paymentMethod(booking.getPaymentMethod())
            .build();
   }

   @Override
   @Transactional
   public void confirmBooking(UUID bookingId, String paymentMethod, String providerTransactionId, String metadata) {
      log.info("Confirming booking: {} via paymentMethod: {}, providerTransactionId: {}", bookingId, paymentMethod, providerTransactionId);
      Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      if (booking.getStatus() == BookingStatus.CONFIRMED) {
         log.info("Booking {} is already CONFIRMED, skipping status update.", bookingId);
         return;
      }

      BookingStatus oldStatus = booking.getStatus();
      booking.setStatus(BookingStatus.CONFIRMED);
      booking = bookingRepository.save(booking);

      // Create Booking Status Log
      BookingStatusLog logEntry = BookingStatusLog.builder()
            .booking(booking)
            .oldStatus(oldStatus)
            .newStatus(BookingStatus.CONFIRMED)
            .reason("Deposit paid via " + paymentMethod)
            .changedBy(booking.getUser())
            .build();
      bookingStatusLogRepository.save(logEntry);

      // Save Transaction record
      Transaction transaction = Transaction.builder()
            .booking(booking)
            .amount(booking.getDepositAmount())
            .transactionType(com.omnibooking.model.enums.TransactionType.PAYMENT)
            .paymentMethod(paymentMethod)
            .status(com.omnibooking.model.enums.TransactionStatus.SUCCESS)
            .providerTransactionId(providerTransactionId)
            .metadata(metadata)
            .build();
      transactionRepository.save(transaction);

      // Send confirmation email
      String token = null;
      if (booking.getUser() != null && Boolean.FALSE.equals(booking.getUser().getIsActive())) {
         token = verificationService.createVerificationToken(booking.getUser().getId());
      }

      String bookingCode = booking.getId().toString().substring(0, 8).toUpperCase();
      String bookingCurrency = booking.getCurrency();
      BigDecimal convertedTotal = currencyService.convertFromBase(booking.getTotalPrice(), bookingCurrency);
      BigDecimal convertedFinal = currencyService.convertFromBase(booking.getFinalPrice(), bookingCurrency);

      String formattedTotal = formatCurrency(convertedTotal, bookingCurrency);
      String formattedFinal = formatCurrency(convertedFinal, bookingCurrency);

      String secondaryTotal = null;
      String secondaryFinal = null;
      if ("VND".equalsIgnoreCase(bookingCurrency)) {
         secondaryTotal = "(" + formatCurrency(booking.getTotalPrice(), "USD") + ")";
         secondaryFinal = "(" + formatCurrency(booking.getFinalPrice(), "USD") + ")";
      }

      EmailEvent emailEvent = mailService.buildBookingConfirmationEmailEvent(
            booking.getGuestEmail(),
            booking.getGuestName(),
            token,
            bookingCode,
            booking.getRoomType().getProperty().getName(),
            booking.getRoomType().getName(),
            booking.getCheckInDate().toString(),
            booking.getCheckOutDate().toString(),
            formattedTotal,
            formattedFinal,
            secondaryTotal,
            secondaryFinal);

      outboxService.saveEvent(
            booking.getId(),
            "BOOKING",
            "BOOKING_CONFIRMED_MAIL",
            emailEvent);

      // Evict partner bookings list cache to show the new booking on their management page
      try {
         org.springframework.cache.Cache cache = cacheManager.getCache("partner_bookings");
         if (cache != null) {
            cache.evict(booking.getRoomType().getProperty().getOwner().getId());
            log.info("Evicted partner_bookings cache for partner: {}", booking.getRoomType().getProperty().getOwner().getId());
         }
      } catch (Exception e) {
         log.error("Failed to evict partner_bookings cache", e);
      }
   }

   @Override
   @org.springframework.transaction.annotation.Transactional(readOnly = true)
   public BookingResponse getBookingById(UUID bookingId) {
      log.info("Fetching booking details for ID: {}", bookingId);
      Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      String bookingCode = booking.getId().toString().substring(0, 8).toUpperCase();

      String token = null;
      if (booking.getUser() != null && Boolean.FALSE.equals(booking.getUser().getIsActive())) {
         token = verificationService.createVerificationToken(booking.getUser().getId());
      }

      return BookingResponse.builder()
            .id(booking.getId())
            .bookingCode(bookingCode)
            .guestName(booking.getGuestName())
            .guestEmail(booking.getGuestEmail())
            .propertyName(booking.getRoomType().getProperty().getName())
            .roomTypeName(booking.getRoomType().getName())
            .checkInDate(booking.getCheckInDate())
            .checkOutDate(booking.getCheckOutDate())
            .numRooms(booking.getNumRooms())
            .totalPrice(booking.getTotalPrice())
            .finalPrice(booking.getFinalPrice())
            .status(booking.getStatus())
            .activationToken(token)
            .currency(booking.getCurrency())
            .depositAmount(booking.getDepositAmount())
            .requiresDeposit(booking.getRequiresDeposit())
            .paymentMethod(booking.getPaymentMethod())
            .build();
   }

   private String formatCurrency(BigDecimal amount, String currency) {
      if ("VND".equalsIgnoreCase(currency)) {
         NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
         return nf.format(amount.setScale(0, RoundingMode.HALF_UP)) + " VND";
      } else {
         NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
         return nf.format(amount.setScale(2, RoundingMode.HALF_UP));
      }
   }
}
