package com.omnibooking.services.pricing.impl;

import com.omnibooking.config.RedisConfig;
import com.omnibooking.model.Coupon;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.services.pricing.CouponService;
import com.omnibooking.services.pricing.AuditLogService;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

   private final CouponRepository couponRepository;

   private final AuditLogService auditLogService;

   private final CacheManager cacheManager;

   public CouponServiceImpl(
         CouponRepository couponRepository,
         AuditLogService auditLogService,
         CacheManager cacheManager) {
      this.couponRepository = couponRepository;
      this.auditLogService = auditLogService;
      this.cacheManager = cacheManager;
   }

   private void evictPricingCache() {
      var cache = cacheManager.getCache(RedisConfig.PROPERTY_PRICING);
      if (cache != null) {
         cache.clear();
      }
   }

   @Override
   public Coupon createCoupon(Coupon coupon, UUID actorId) {
      Coupon saved = couponRepository.save(coupon);
      auditLogService.logChange("COUPON", saved.getId(), "CREATE", actorId, null, saved);
      evictPricingCache();
      return saved;
   }

   @Override
   public Coupon updateCoupon(UUID couponId, Coupon couponDetails, UUID actorId) {
      Coupon existing = couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

      // Clone old values for audit log
      Coupon oldCopy = Coupon.builder()
            .code(existing.getCode())
            .discountType(existing.getDiscountType())
            .discountValue(existing.getDiscountValue())
            .minBookingAmount(existing.getMinBookingAmount())
            .maxDiscountAmount(existing.getMaxDiscountAmount())
            .validFrom(existing.getValidFrom())
            .validUntil(existing.getValidUntil())
            .usageLimit(existing.getUsageLimit())
            .usedCount(existing.getUsedCount())
            .reservedCount(existing.getReservedCount())
            .isActive(existing.getIsActive())
            .property(existing.getProperty())
            .build();
      oldCopy.setId(existing.getId());

      // Update fields
      existing.setCode(couponDetails.getCode());
      existing.setDiscountType(couponDetails.getDiscountType());
      existing.setDiscountValue(couponDetails.getDiscountValue());
      existing.setMinBookingAmount(couponDetails.getMinBookingAmount());
      existing.setMaxDiscountAmount(couponDetails.getMaxDiscountAmount());
      existing.setValidFrom(couponDetails.getValidFrom());
      existing.setValidUntil(couponDetails.getValidUntil());
      existing.setUsageLimit(couponDetails.getUsageLimit());
      existing.setIsActive(couponDetails.getIsActive());
      existing.setProperty(couponDetails.getProperty());

      Coupon updated = couponRepository.save(existing);
      auditLogService.logChange("COUPON", updated.getId(), "UPDATE", actorId, oldCopy, updated);
      evictPricingCache();
      return updated;
   }

   @Override
   public void deleteCoupon(UUID couponId, UUID actorId) {
      Coupon existing = couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

      auditLogService.logChange("COUPON", couponId, "DELETE", actorId, existing, null);
      couponRepository.deleteById(couponId);
      evictPricingCache();
   }

   @Override
   @Transactional(readOnly = true)
   public List<Coupon> getCouponsByProperty(UUID propertyId) {
      return couponRepository.findByPropertyId(propertyId);
   }

   @Override
   @Transactional(readOnly = true)
   public Coupon getCouponById(UUID couponId) {
      return couponRepository.findById(couponId)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
   }

   @Override
   @Transactional(readOnly = true)
   public Coupon getCouponByCode(String code) {
      return couponRepository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
   }

}
