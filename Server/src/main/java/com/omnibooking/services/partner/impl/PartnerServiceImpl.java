package com.omnibooking.services.partner.impl;

import com.omnibooking.dto.PartnerStatsResponse;
import com.omnibooking.model.Booking;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.services.partner.PartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.omnibooking.dto.PartnerBookingResponse;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.services.core.EncryptionService;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerServiceImpl implements PartnerService {

   private final BookingRepository bookingRepository;

   private final ReviewRepository reviewRepository;

   private final EncryptionService encryptionService;

   @Override
   @Transactional(readOnly = true)
   public PartnerStatsResponse getPartnerStats(UUID partnerId) {
      log.info("Calculating partner stats for partner: {}", partnerId);

      List<Booking> bookings = bookingRepository.findAllByPartnerId(partnerId);

      LocalDate now = LocalDate.now();
      LocalDate startOfCurrentMonth = now.withDayOfMonth(1);
      LocalDate startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
      LocalDate endOfPreviousMonth = startOfCurrentMonth.minusDays(1);

      List<Booking> currentMonthBookings = new ArrayList<>();
      List<Booking> previousMonthBookings = new ArrayList<>();

      for (Booking b : bookings) {
         if (b.getCreatedAt() == null)
            continue;
         LocalDate bDate = LocalDate.ofInstant(b.getCreatedAt(), ZoneId.systemDefault());

         boolean isActive = b.getStatus() != BookingStatus.CANCELLED && b.getStatus() != BookingStatus.REFUNDED;

         if (isActive) {
            if (!bDate.isBefore(startOfCurrentMonth) && !bDate.isAfter(now)) {
               currentMonthBookings.add(b);
            } else if (!bDate.isBefore(startOfPreviousMonth) && !bDate.isAfter(endOfPreviousMonth)) {
               previousMonthBookings.add(b);
            }
         }
      }

      // Monthly Revenue
      BigDecimal currentRevenue = currentMonthBookings.stream()
            .map(Booking::getFinalPrice)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal previousRevenue = previousMonthBookings.stream()
            .map(Booking::getFinalPrice)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

      String revenueStr = formatRevenue(currentRevenue);
      String revenueChange = calculatePercentageChange(currentRevenue, previousRevenue);
      boolean revenueUp = currentRevenue.compareTo(previousRevenue) >= 0;

      // Total Bookings
      long currentBookingCount = currentMonthBookings.size();
      long previousBookingCount = previousMonthBookings.size();
      String bookingsStr = String.valueOf(currentBookingCount);
      String bookingsChange = calculateCountPercentageChange(currentBookingCount, previousBookingCount);
      boolean bookingsUp = currentBookingCount >= previousBookingCount;

      // New Customers
      long currentCustomers = currentMonthBookings.stream()
            .map(Booking::getGuestEmail)
            .filter(Objects::nonNull)
            .distinct()
            .count();

      long previousCustomers = previousMonthBookings.stream()
            .map(Booking::getGuestEmail)
            .filter(Objects::nonNull)
            .distinct()
            .count();

      String customersStr = String.valueOf(currentCustomers);
      String customersChange = calculateCountPercentageChange(currentCustomers, previousCustomers);
      boolean customersUp = currentCustomers >= previousCustomers;

      // Rating Score
      Double avgRating = reviewRepository.getAverageRatingByOwnerId(partnerId);
      Double ratingScoreVal = null;
      if (avgRating != null) {
         ratingScoreVal = BigDecimal.valueOf(avgRating)
               .setScale(1, RoundingMode.HALF_UP)
               .doubleValue();
      }
      String ratingChange = "+0.0";
      boolean ratingUp = true;

      return PartnerStatsResponse.builder()
            .monthlyRevenue(revenueStr)
            .monthlyRevenueChange(revenueChange)
            .monthlyRevenueUp(revenueUp)
            .totalBookings(bookingsStr)
            .totalBookingsChange(bookingsChange)
            .totalBookingsUp(bookingsUp)
            .newCustomers(customersStr)
            .newCustomersChange(customersChange)
            .newCustomersUp(customersUp)
            .ratingScore(ratingScoreVal)
            .ratingScoreChange(ratingChange)
            .ratingScoreUp(ratingUp)
            .build();
   }

   private String formatRevenue(BigDecimal revenue) {
      if (revenue.compareTo(BigDecimal.ZERO) == 0) {
         return "0";
      }
      if (revenue.compareTo(new BigDecimal("1000000")) >= 0) {
         BigDecimal millions = revenue.divide(new BigDecimal("1000000"), 1, RoundingMode.HALF_UP);
         return "$" + millions.toString() + "M";
      } else if (revenue.compareTo(new BigDecimal("1000")) >= 0) {
         BigDecimal thousands = revenue.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP);
         return "$" + thousands.toString() + "K";
      } else {
         return "$" + revenue.setScale(0, RoundingMode.HALF_UP).toString();
      }
   }

   private String calculatePercentageChange(BigDecimal current, BigDecimal previous) {
      if (previous.compareTo(BigDecimal.ZERO) == 0) {
         return current.compareTo(BigDecimal.ZERO) == 0 ? "0.0%" : "+100.0%";
      }
      BigDecimal difference = current.subtract(previous);
      BigDecimal change = difference.multiply(new BigDecimal("100"))
            .divide(previous, 1, RoundingMode.HALF_UP);

      String prefix = change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
      return prefix + change.toString() + "%";
   }

   private String calculateCountPercentageChange(long current, long previous) {
      if (previous == 0) {
         return current == 0 ? "0.0%" : "+100.0%";
      }
      double change = ((double) (current - previous) / previous) * 100;
      String prefix = change >= 0 ? "+" : "";
      return prefix + String.format(Locale.US, "%.1f", change) + "%";
   }

   @Override
   @Transactional(readOnly = true)
   @Cacheable(value = "partner_bookings", key = "#partnerId")
   public List<PartnerBookingResponse> getPartnerBookings(UUID partnerId) {
      log.info("Fetching and caching bookings list for partner: {}", partnerId);

      List<Booking> bookings = bookingRepository.findAllByPartnerId(partnerId);

      // Sort bookings: newest bookings first (using createdAt, fallback to
      // checkInDate)
      bookings.sort((b1, b2) -> {
         if (b1.getCreatedAt() != null && b2.getCreatedAt() != null) {
            return b2.getCreatedAt().compareTo(b1.getCreatedAt()); // Descending
         }
         return b2.getCheckInDate().compareTo(b1.getCheckInDate());
      });

      List<PartnerBookingResponse> response = new ArrayList<>();
      for (Booking b : bookings) {
         String decryptedPhone = b.getGuestPhoneEncrypted() != null
               ? encryptionService.decrypt(b.getGuestPhoneEncrypted())
               : null;

         response.add(PartnerBookingResponse.builder()
               .id(b.getId())
               .propertyName(b.getRoomType().getProperty().getName())
               .roomTypeName(b.getRoomType().getName())
               .checkInDate(b.getCheckInDate())
               .checkOutDate(b.getCheckOutDate())
               .numRooms(b.getNumRooms())
               .totalPrice(b.getTotalPrice())
               .finalPrice(b.getFinalPrice())
               .status(b.getStatus())
               .guestName(b.getGuestName())
               .guestEmail(b.getGuestEmail())
               .guestPhone(decryptedPhone)
               .specialRequests(b.getSpecialRequests())
               .createdAt(b.getCreatedAt())
               .build());
      }
      return response;
   }

}
