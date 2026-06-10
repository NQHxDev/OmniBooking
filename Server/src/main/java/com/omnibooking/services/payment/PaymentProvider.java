package com.omnibooking.services.payment;

import java.util.Map;
import java.util.UUID;

import com.omnibooking.model.Transaction;
import com.omnibooking.model.enums.PaymentGatewayStatus;

public interface PaymentProvider {

   String getProviderName();

   String createPaymentLink(UUID bookingId);

   void processPaymentCallback(Map<String, String> params);

   PaymentGatewayStatus queryPaymentStatus(Transaction transaction);

}
