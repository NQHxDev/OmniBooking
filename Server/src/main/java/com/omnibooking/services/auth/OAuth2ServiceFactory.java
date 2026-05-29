package com.omnibooking.services.auth;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OAuth2ServiceFactory {

   private final Map<String, OAuth2ProviderService> services;

   public OAuth2ServiceFactory(List<OAuth2ProviderService> providerServices) {
      services = providerServices.stream()
            .collect(Collectors.toMap(
                  service -> service.getProviderName().toLowerCase(),
                  Function.identity()));
   }

   public OAuth2ProviderService getService(String provider) {
      OAuth2ProviderService service = services.get(provider.toLowerCase());
      if (service == null) {
         throw new RuntimeException("OAuth2 provider not supported: " + provider);
      }
      return service;
   }
}
