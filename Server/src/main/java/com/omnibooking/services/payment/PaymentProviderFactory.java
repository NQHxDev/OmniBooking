package com.omnibooking.services.payment;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProviderFactory {

   private final Map<String, PaymentProvider> providers;

   public PaymentProviderFactory(List<PaymentProvider> providerList) {
      this.providers = providerList.stream()
            .collect(Collectors.toMap(
                  p -> p.getProviderName().toUpperCase(),
                  Function.identity()));
   }

   public PaymentProvider getProvider(String providerName) {
      if (providerName == null) {
         throw new IllegalArgumentException("Payment provider name cannot be null");
      }
      PaymentProvider provider = providers.get(providerName.toUpperCase());
      if (provider == null) {
         throw new IllegalArgumentException("Unsupported payment provider: " + providerName);
      }
      return provider;
   }

}
