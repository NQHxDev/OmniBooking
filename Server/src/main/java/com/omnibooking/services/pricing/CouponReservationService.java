package com.omnibooking.services.pricing;

import com.omnibooking.model.CouponReservation;
import java.util.UUID;

public interface CouponReservationService {

   CouponReservation reserveCoupon(UUID couponId, String bookingSessionId, UUID customerId, UUID propertyId);

   void consumeReservation(String reservationToken);

   void releaseReservation(String reservationToken);

   void refundReservation(UUID couponId, UUID customerId);

   void cleanExpiredReservations();

}
