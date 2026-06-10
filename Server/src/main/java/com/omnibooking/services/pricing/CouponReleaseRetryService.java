package com.omnibooking.services.pricing;

import java.util.UUID;

public interface CouponReleaseRetryService {

   void createRetry(UUID bookingId, UUID couponId, UUID userId);

   void processPendingRetries();

   void purgeOldRetryRecords();
}
