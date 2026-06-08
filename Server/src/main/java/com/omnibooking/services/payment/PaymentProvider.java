package com.omnibooking.services.payment;

import java.util.Map;
import java.util.UUID;

public interface PaymentProvider {

   String getProviderName();

   String createPaymentLink(UUID bookingId);

   void processPaymentCallback(Map<String, String> params);

}
