package com.omnibooking.services.pricing;

import com.omnibooking.model.Coupon;
import java.util.List;
import java.util.UUID;

public interface CouponService {

   Coupon createCoupon(Coupon coupon, UUID actorId);

   Coupon updateCoupon(UUID couponId, Coupon couponDetails, UUID actorId);

   void deleteCoupon(UUID couponId, UUID actorId);

   List<Coupon> getCouponsByProperty(UUID propertyId);

   Coupon getCouponById(UUID couponId);

   Coupon getCouponByCode(String code);

}
