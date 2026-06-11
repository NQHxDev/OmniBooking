package com.omnibooking.services.booking.impl;

import com.omnibooking.constant.EventConstants;
import com.omnibooking.constant.SecurityConstants;
import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.dto.BookingResponse;
import com.omnibooking.dto.event.EmailEvent;
import com.omnibooking.exception.AppException;
import com.omnibooking.exception.ErrorCode;
import com.omnibooking.model.Booking;
import com.omnibooking.model.BookingAppliedRuleVersion;
import com.omnibooking.model.BookingPriceBreakdown;
import com.omnibooking.model.BookingStatusLog;
import com.omnibooking.model.Coupon;
import com.omnibooking.model.Role;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.model.enums.ReservationStatus;
import com.omnibooking.model.enums.RuleType;
import com.omnibooking.model.enums.TransactionStatus;
import com.omnibooking.model.enums.TransactionType;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.user.UserProfileRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.pricing.PriceRuleVersionRepository;
import com.omnibooking.model.Transaction;
import com.omnibooking.model.PaymentEvent;
import com.omnibooking.repository.payment.PaymentEventRepository;
import com.omnibooking.services.payment.PaymentStateMachine;
import com.omnibooking.security.UserPrincipal;
import com.omnibooking.services.booking.BookingService;
import com.omnibooking.services.communication.MailService;
import com.omnibooking.services.core.BloomFilterService;
import com.omnibooking.services.core.CurrencyService;
import com.omnibooking.services.core.EncryptionService;
import com.omnibooking.services.core.OutboxService;
import com.omnibooking.services.pricing.CouponReleaseRetryService;
import com.omnibooking.services.pricing.CouponReservationService;
import com.omnibooking.services.pricing.PriceCalculationService;
import com.omnibooking.services.pricing.PricingEngine;
import com.omnibooking.services.pricing.PricingRuleHandler;
import com.omnibooking.services.user.VerificationService;
import com.omnibooking.services.auth.CachedRoleService;
import com.omnibooking.services.booking.BookingStateMachine;
import com.omnibooking.services.booking.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.omnibooking.config.BookingConfigProperties;
import com.omnibooking.model.PriceRuleVersion;
import com.omnibooking.services.pricing.PriceCalculationService.StayPriceResult;
import io.micrometer.core.instrument.Counter;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.Cache;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

   private final BookingRepository bookingRepository;

   private final RoomTypeRepository roomTypeRepository;

   private final RoomAvailabilityRepository roomAvailabilityRepository;

   private final UserRepository userRepository;

   private final UserProfileRepository userProfileRepository;

   private final CachedRoleService cachedRoleService;

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

   private final PriceCalculationService priceCalculationService;

   private final CouponReservationService couponReservationService;

   private final CouponReservationRepository couponReservationRepository;

   private final BookingPriceBreakdownRepository bookingPriceBreakdownRepository;

   private final BookingAppliedRuleVersionRepository bookingAppliedRuleVersionRepository;

   private final PriceRuleVersionRepository priceRuleVersionRepository;

   private final PricingEngine pricingEngine;

   private final BookingStateMachine bookingStateMachine;

   private final InventoryService inventoryService;

   private final BookingConfigProperties bookingConfig;

   private final CouponReleaseRetryService couponReleaseRetryService;

   private final Counter bookingCreatedCounter;

   private final Counter bookingConfirmedCounter;

   private final Counter bookingCancelledCounter;

   private final Counter paymentCallbackCounter;

   private final Counter paymentDuplicateCallbackCounter;

   private final PaymentStateMachine paymentStateMachine;

   private final PaymentEventRepository paymentEventRepository;

   @Override
   @Transactional
   public BookingResponse createBooking(CreateBookingRequest request, UserPrincipal principal) {
      if (request.getCheckInDate().isAfter(request.getCheckOutDate())
            || request.getCheckInDate().isEqual(request.getCheckOutDate())) {
         throw new AppException("BOOKING_002", "Check-in date must be before check-out date", HttpStatus.BAD_REQUEST);
      }

      RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Room type not found"));

      checkQuickAvailability(roomType, request.getCheckInDate(), request.getCheckOutDate(), request.getNumRooms());

      // Resolve User (Logged in vs. Guest)
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
            Role userRole = cachedRoleService.getRoleByName(SecurityConstants.Roles.USER);

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

      // Get guest count (defaults to room type capacities)
      int guestCount = request.getGuestCount() != null ? request.getGuestCount()
            : (roomType.getCapacityAdults()
                  + (roomType.getCapacityChildren() != null ? roomType.getCapacityChildren() : 0));

      // Resolve Coupon and Coupon Reservation
      String couponCode = null;
      Coupon coupon = null;

      if (request.getReservationToken() != null && !request.getReservationToken().trim().isEmpty()) {
         String token = request.getReservationToken().trim();
         var reservation = couponReservationRepository.findByReservationToken(token)
               .orElseThrow(
                     () -> new AppException("BOOKING_003", "Coupon reservation not found", HttpStatus.BAD_REQUEST));
         if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException("BOOKING_003", "Coupon reservation is not active", HttpStatus.BAD_REQUEST);
         }
         coupon = reservation.getCoupon();
         couponCode = coupon.getCode();

         // Consume the reservation atomically
         couponReservationService.consumeReservation(token);
      } else if (request.getCouponId() != null) {
         // Fallback/Legacy API support: atomically reserve and consume
         coupon = couponRepository.findById(request.getCouponId())
               .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Coupon not found"));
         couponCode = coupon.getCode();

         int reserved = couponRepository.incrementReservedCountAtomically(coupon.getId());
         if (reserved == 0) {
            throw new AppException("BOOKING_005", "Coupon usage limit reached or coupon inactive",
                  HttpStatus.BAD_REQUEST);
         }
         int consumed = couponRepository.consumeReservedCouponAtomically(coupon.getId());
         if (consumed == 0) {
            throw new AppException("BOOKING_005", "Coupon consumption failed", HttpStatus.BAD_REQUEST);
         }
      }

      // Calculate stay price with coupon
      StayPriceResult stayPrice = priceCalculationService
            .calculateStayPriceWithCoupon(
                  roomType.getProperty().getId(),
                  roomType.getId(),
                  request.getCheckInDate(),
                  request.getCheckOutDate(),
                  guestCount,
                  couponCode);

      BigDecimal numRoomsDec = BigDecimal.valueOf(request.getNumRooms());
      BigDecimal totalPrice = stayPrice.totalBasePrice()
            .add(stayPrice.totalSeasonalAdjustment())
            .add(stayPrice.totalWeekendAdjustment())
            .add(stayPrice.totalOccupancyAdjustment())
            .multiply(numRoomsDec);
      BigDecimal finalPrice = stayPrice.totalFinalPrice().multiply(numRoomsDec);

      // Calculate Deposit Requirements
      boolean requiresDeposit = false;
      if (principal == null) {
         requiresDeposit = true;
      } else {
         long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), request.getCheckInDate());
         if (daysBetween >= 5) {
            requiresDeposit = true;
         }
      }

      BigDecimal depositAmount = BigDecimal.ZERO;
      if (requiresDeposit) {
         BigDecimal fifteenPercent = finalPrice.multiply(new BigDecimal("0.15"));
         BigDecimal firstNightRoomPrice = roomType.getBasePrice();
         Optional<RoomAvailability> firstDayOpt = roomAvailabilityRepository
               .findByRoomTypeIdAndAvailabilityDate(roomType.getId(), request.getCheckInDate());
         if (firstDayOpt.isPresent() && firstDayOpt.get().getPriceOverride() != null) {
            firstNightRoomPrice = firstDayOpt.get().getPriceOverride();
         }
         BigDecimal oneNightTotal = firstNightRoomPrice.multiply(BigDecimal.valueOf(request.getNumRooms()));
         depositAmount = fifteenPercent.min(oneNightTotal).setScale(4, RoundingMode.HALF_UP);
      }

      // Save Booking
      boolean isPendingOnlinePayment = requiresDeposit && ("momo".equalsIgnoreCase(request.getPaymentMethod())
            || "visa".equalsIgnoreCase(request.getPaymentMethod()));
      BookingStatus initialStatus = isPendingOnlinePayment ? BookingStatus.PENDING_PAYMENT : BookingStatus.CONFIRMED;

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

      if (initialStatus == BookingStatus.PENDING_PAYMENT) {
         booking.setExpiresAt(Instant.now().plus(bookingConfig.getHoldDurationMinutes(), ChronoUnit.MINUTES));
      }

      // Room Availability Check & Locks (Deduct first to hold locks, ensuring
      // correctness before booking is saved)
      inventoryService.deductInventoryOnly(roomType, request.getCheckInDate(), request.getCheckOutDate(),
            request.getNumRooms());

      booking = bookingRepository.saveAndFlush(booking);

      // Write Reserve Audit Log after booking is saved (having a valid database ID)
      inventoryService.writeReserveAuditLog(booking, roomType, request.getCheckInDate(), request.getCheckOutDate(),
            request.getNumRooms());

      // Collect all rule IDs for batch load
      Set<UUID> allRuleIds = stayPrice.dailyPrices().stream()
            .flatMap(dp -> dp.appliedRuleIds().stream())
            .collect(Collectors.toSet());

      // 1 batch query replaces 2N individual queries
      Map<UUID, PriceRuleVersion> latestVersions = priceRuleVersionRepository
            .findLatestVersionsByPriceRuleIds(allRuleIds)
            .stream()
            .collect(Collectors.toMap(v -> v.getPriceRule().getId(), v -> v));

      // Save Booking Price Breakdown and Applied Rule Versions using optimized
      // saveAll batch inserts
      List<BookingPriceBreakdown> breakdowns = new ArrayList<>();
      List<BookingAppliedRuleVersion> appliedRuleVersions = new ArrayList<>();

      for (var dp : stayPrice.dailyPrices()) {
         BigDecimal dayBase = dp.basePrice().multiply(numRoomsDec);
         BigDecimal daySeasonal = dp.seasonalAdjustment().multiply(numRoomsDec);
         BigDecimal dayWeekend = dp.weekendAdjustment().multiply(numRoomsDec);
         BigDecimal dayOccupancy = dp.occupancyAdjustment().multiply(numRoomsDec);
         BigDecimal preCouponDayPrice = dayBase.add(daySeasonal).add(dayWeekend).add(dayOccupancy);
         BigDecimal dayFinal = dp.finalPrice().multiply(numRoomsDec);
         BigDecimal dayDiscount = preCouponDayPrice.subtract(dayFinal);

         UUID breakdownId = UuidCreator.getTimeOrderedEpoch();
         BookingPriceBreakdown breakdown = BookingPriceBreakdown.builder()
               .id(breakdownId)
               .booking(booking)
               .stayDate(dp.date())
               .basePrice(dayBase)
               .seasonalAdjustment(daySeasonal)
               .weekendAdjustment(dayWeekend)
               .occupancyAdjustment(dayOccupancy)
               .couponDiscount(dayDiscount)
               .finalPrice(dayFinal)
               .appliedCouponId(coupon != null ? coupon.getId() : null)
               .appliedCouponCode(coupon != null ? coupon.getCode() : null)
               .build();
         breakdowns.add(breakdown);

         for (UUID ruleId : dp.appliedRuleIds()) {
            PriceRuleVersion ruleVer = latestVersions.get(ruleId);
            if (ruleVer != null) {
               BigDecimal ruleAdjustment = BigDecimal.ZERO;
               if (ruleVer.getRuleType() == RuleType.SEASONAL) {
                  ruleAdjustment = daySeasonal;
               } else if (ruleVer.getRuleType() == RuleType.WEEKEND) {
                  ruleAdjustment = dayWeekend;
               } else if (ruleVer.getRuleType() == RuleType.OCCUPANCY) {
                  ruleAdjustment = dayOccupancy;
               }

               if (ruleAdjustment.compareTo(BigDecimal.ZERO) == 0) {
                  PricingRuleHandler handler = pricingEngine.getHandler(ruleVer.getRuleType());
                  ruleAdjustment = handler
                        .calculateAdjustment(ruleVer.getAdjustmentType(), ruleVer.getAdjustmentValue(), dp.basePrice())
                        .multiply(numRoomsDec);
               }

               if (ruleAdjustment.compareTo(BigDecimal.ZERO) != 0) {
                  UUID appliedRuleId = UuidCreator.getTimeOrderedEpoch();
                  BookingAppliedRuleVersion appliedRuleVer = BookingAppliedRuleVersion.builder()
                        .id(appliedRuleId)
                        .bookingPriceBreakdown(breakdown)
                        .priceRuleVersion(ruleVer)
                        .adjustmentAmount(ruleAdjustment)
                        .build();
                  appliedRuleVersions.add(appliedRuleVer);
               }
            }
         }
      }

      bookingPriceBreakdownRepository.saveAll(breakdowns);
      bookingAppliedRuleVersionRepository.saveAll(appliedRuleVersions);

      // Create Booking Status Log
      BookingStatusLog logEntry = BookingStatusLog.builder()
            .booking(booking)
            .oldStatus(null)
            .newStatus(initialStatus)
            .reason(isPendingOnlinePayment ? "Initial booking created, pending MoMo deposit payment"
                  : "Initial booking created and confirmed")
            .changedBy(user)
            .build();
      bookingStatusLogRepository.save(logEntry);

      // Send outbox confirmation email & activation token if guest is new or inactive
      String token = null;
      if (isNewGuest || isInactiveGuest) {
         token = verificationService.createVerificationToken(user.getId());
      }

      String bookingCode = booking.getId().toString().substring(0, 8).toUpperCase();

      if (!isPendingOnlinePayment) {
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
               EventConstants.BOOKING_CONFIRMED_MAIL,
               emailEvent);

         // Evict partner bookings list and stats cache
         evictPartnerCaches(roomType.getProperty().getOwner().getId());
      }

      bookingCreatedCounter.increment();

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
      paymentCallbackCounter.increment();

      log.info("Acquiring pessimistic write lock for booking {}", bookingId);
      Booking booking = bookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
         log.info("Booking {} not in PENDING_PAYMENT state (current: {}), skipping confirmation", bookingId,
               booking.getStatus());
         return;
      }

      // Transition booking status to CONFIRMED
      BookingStatus oldStatus = booking.getStatus();
      booking.setStatus(BookingStatus.CONFIRMED);
      booking.setExpiresAt(null);
      bookingRepository.save(booking);

      // Save booking status log
      bookingStatusLogRepository.save(BookingStatusLog.builder()
            .booking(booking)
            .oldStatus(oldStatus)
            .newStatus(BookingStatus.CONFIRMED)
            .reason("Deposit paid via " + paymentMethod)
            .changedBy(booking.getUser())
            .build());

      // Parse orderId from metadata to update the pending transaction
      String orderId = null;
      if (metadata != null) {
         try {
            JsonNode rootNode = new ObjectMapper().readTree(metadata);
            if (rootNode.has("orderId")) {
               orderId = rootNode.get("orderId").asText();
            }
         } catch (Exception e) {
            log.warn("Failed to parse orderId from metadata JSON: {}", e.getMessage());
         }
      }

      Transaction transaction;
      if (orderId != null) {
         Optional<Transaction> pendingTxOpt = transactionRepository.findByProviderOrderId(orderId);
         if (pendingTxOpt.isPresent()) {
            transaction = pendingTxOpt.get();
            paymentStateMachine.transition(transaction, TransactionStatus.SUCCESS);
            transaction.setProviderTransactionId(providerTransactionId);
            transaction.setMetadata(metadata);
         } else {
            transaction = Transaction.builder()
                  .booking(booking)
                  .amount(booking.getDepositAmount())
                  .localAmount(booking.getDepositAmount()) // fallback
                  .localCurrency(booking.getCurrency())
                  .transactionType(TransactionType.PAYMENT)
                  .paymentMethod(paymentMethod)
                  .status(TransactionStatus.SUCCESS)
                  .providerTransactionId(providerTransactionId)
                  .metadata(metadata)
                  .build();
         }
      } else {
         transaction = Transaction.builder()
               .booking(booking)
               .amount(booking.getDepositAmount())
               .localAmount(booking.getDepositAmount()) // fallback
               .localCurrency(booking.getCurrency())
               .transactionType(TransactionType.PAYMENT)
               .paymentMethod(paymentMethod)
               .status(TransactionStatus.SUCCESS)
               .providerTransactionId(providerTransactionId)
               .metadata(metadata)
               .build();
      }

      try {
         transaction = transactionRepository.save(transaction);

         // Record PAYMENT_CONFIRMED event
         PaymentEvent confEvent = PaymentEvent.builder()
               .transactionId(transaction.getId())
               .bookingId(bookingId)
               .eventType("PAYMENT_CONFIRMED")
               .metadata(metadata)
               .build();
         paymentEventRepository.save(confEvent);
      } catch (DataIntegrityViolationException ex) {
         log.info("Duplicate provider transaction ignored: providerTxId={}", providerTransactionId);
         paymentDuplicateCallbackCounter.increment();
         return;
      }

      // STEP 5: Outbox event (in same TX) — guaranteed email delivery (only once)
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
            EventConstants.BOOKING_CONFIRMED_MAIL,
            emailEvent);

      // STEP 6: Metrics (only once)
      bookingConfirmedCounter.increment();

      // Evict partner bookings list and stats cache
      evictPartnerCaches(booking.getRoomType().getProperty().getOwner().getId());
   }

   @Override
   @Transactional
   public void cancelBooking(UUID bookingId, String reason, User changedBy) {
      Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Booking not found"));

      // State machine validates: only PENDING_PAYMENT or CONFIRMED can be cancelled
      bookingStateMachine.transition(booking, BookingStatus.CANCELLED, reason, changedBy);
      booking = bookingRepository.save(booking);

      // Release inventory (only after successful transition)
      inventoryService.releaseInventory(booking);

      // Release coupon in same transaction
      if (booking.getCoupon() != null && booking.getUser() != null) {
         UUID couponId = booking.getCoupon().getId();
         UUID userId = booking.getUser().getId();
         try {
            couponReservationService.refundReservation(couponId, userId);
         } catch (Exception e) {
            log.warn("Immediate coupon release failed for cancelled booking {}, persisting retry record.",
                  booking.getId(), e);
            couponReleaseRetryService.createRetry(booking.getId(), couponId, userId);
         }
      }

      bookingCancelledCounter.increment();
      evictPartnerCaches(booking.getRoomType().getProperty().getOwner().getId());
   }

   @Override
   @Transactional(readOnly = true)
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

   @Override
   @Transactional(readOnly = true)
   public List<BookingResponse> getMyBookings(UUID userId) {
      log.info("Fetching bookings for user ID: {}", userId);
      return bookingRepository.findByUserId(userId).stream()
            .map(booking -> {
               String bookingCode = booking.getId().toString().substring(0, 8).toUpperCase();
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
                     .currency(booking.getCurrency())
                     .depositAmount(booking.getDepositAmount())
                     .requiresDeposit(booking.getRequiresDeposit())
                     .paymentMethod(booking.getPaymentMethod())
                     .build();
            })
            .toList();
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

   private void checkQuickAvailability(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int numRooms) {
      if (numRooms > roomType.getTotalRooms()) {
         throw new AppException("BOOKING_001", "Requested number of rooms exceeds total rooms", HttpStatus.BAD_REQUEST);
      }
      List<RoomAvailability> availabilities = roomAvailabilityRepository.findByRoomTypeIdAndAvailabilityDateRange(
            roomType.getId(), checkIn, checkOut);
      for (RoomAvailability availability : availabilities) {
         if (Boolean.TRUE.equals(availability.getIsClosed()) || availability.getAvailableCount() < numRooms) {
            throw new AppException("BOOKING_001", "Room not available for date: " + availability.getAvailabilityDate(),
                  HttpStatus.BAD_REQUEST);
         }
      }
   }

   private void evictPartnerCaches(UUID partnerId) {
      if (partnerId == null)
         return;
      try {
         Cache bookingsCache = cacheManager.getCache("partner_bookings");
         if (bookingsCache != null) {
            bookingsCache.evict(partnerId);
            log.info("Evicted partner_bookings cache for partner: {}", partnerId);
         }
      } catch (Exception e) {
         log.error("Failed to evict partner_bookings cache", e);
      }
      try {
         Cache statsCache = cacheManager.getCache("partner_stats");
         if (statsCache != null) {
            String currentMonthKey = partnerId.toString() + ":" + YearMonth.now().toString();
            statsCache.evict(currentMonthKey);
            log.info("Evicted partner_stats cache key: {} for partner: {}", currentMonthKey, partnerId);
         }
      } catch (Exception e) {
         log.error("Failed to evict partner_stats cache", e);
      }
   }

}
