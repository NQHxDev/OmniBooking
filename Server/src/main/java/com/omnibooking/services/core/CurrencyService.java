package com.omnibooking.services.core;

import com.omnibooking.config.AppProperties;
import com.omnibooking.model.ExchangeRate;
import com.omnibooking.repository.CurrencyRepository;
import com.omnibooking.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

   private static final String RATE_CACHE_PREFIX = "currency:rate:";

   private final CurrencyRepository currencyRepository;
   private final ExchangeRateRepository exchangeRateRepository;
   private final AppProperties appProperties;
   private final RestTemplate restTemplate;
   private final StringRedisTemplate redisTemplate;

   /**
    * Converts an amount from the base currency (USD) to a target currency.
    */
   public BigDecimal convertFromBase(BigDecimal amount, String toCurrency) {
      if (appProperties.getCurrency().getBaseCurrency().equalsIgnoreCase(toCurrency)) {
         return amount;
      }

      BigDecimal rate = getRate(toCurrency);
      return amount.multiply(rate).setScale(getScaleForCurrency(toCurrency), RoundingMode.HALF_UP);
   }

   /**
    * Gets the exchange rate for a target currency relative to the base currency.
    * Priority: Redis -> DB -> External API (Fallback)
    */
   public BigDecimal getRate(String toCurrency) {
      String cacheKey = RATE_CACHE_PREFIX + toCurrency.toUpperCase();
      String cachedRate = redisTemplate.opsForValue().get(cacheKey);

      if (cachedRate != null) {
         return new BigDecimal(cachedRate);
      }

      // Try DB
      return exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByFetchedAtDesc(
            appProperties.getCurrency().getBaseCurrency(), toCurrency)
            .map(ExchangeRate::getRate)
            .orElseGet(() -> {
               log.warn("Rate not found for currency: {}. Fetching from API...", toCurrency);
               updateRates(); // Update all rates

               // Try to get the newly updated rate from Redis
               String updatedRate = redisTemplate.opsForValue().get(cacheKey);
               if (updatedRate != null)
                  return new BigDecimal(updatedRate);

               return fetchFallbackRate(toCurrency);
            });
   }

   public void updateRates() {
      String base = appProperties.getCurrency().getBaseCurrency();
      String apiKey = appProperties.getCurrency().getApiKey();
      String url = String.format(appProperties.getCurrency().getProviderUrl(), apiKey, base);

      try {
         @SuppressWarnings("unchecked")
         Map<String, Object> response = restTemplate.getForObject(url, Map.class);

         if (response != null && "success".equals(response.get("result"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("conversion_rates");

            // Get list of supported currency codes from DB
            List<String> supportedCodes = currencyRepository.findAll().stream()
                  .map(com.omnibooking.model.Currency::getCode)
                  .collect(Collectors.toList());

            rates.forEach((code, value) -> {
               // Only process if the currency is supported by our system
               if (supportedCodes.contains(code)) {
                  BigDecimal apiRate = new BigDecimal(String.valueOf(value));

                  // Apply profit margin (markup)
                  BigDecimal bigRate = applyProfitMargin(code, apiRate);

                  // Save to DB
                  ExchangeRate exchangeRate = ExchangeRate.builder()
                        .fromCurrency(base)
                        .toCurrency(code)
                        .rate(bigRate)
                        .provider("ExchangeRate-API")
                        .fetchedAt(Instant.now())
                        .build();
                  exchangeRateRepository.save(exchangeRate);

                  // Cache in Redis (TTL 4 hours)
                  redisTemplate.opsForValue().set(RATE_CACHE_PREFIX + code, bigRate.toString(), 4, TimeUnit.HOURS);
               }
            });
            log.info("Successfully updated exchange rates from API for {} supported currencies.",
                  supportedCodes.size());
         }
      } catch (Exception e) {
         log.error("Failed to update exchange rates: {}", e.getMessage());
      }
   }

   private BigDecimal applyProfitMargin(String code, BigDecimal rate) {
      if (code.equalsIgnoreCase("USD"))
         return rate; // Base currency

      if (code.equalsIgnoreCase("VND")) {
         int randomMarkup = ThreadLocalRandom.current().nextInt(300, 1001);
         return rate.add(new BigDecimal(randomMarkup));
      }

      return rate.multiply(new BigDecimal("1.05"))
            .setScale(getScaleForCurrency(code), RoundingMode.HALF_UP);
   }

   private BigDecimal fetchFallbackRate(String toCurrency) {
      if ("VND".equalsIgnoreCase(toCurrency))
         return new BigDecimal("25000"); // Chỉ dùng khi API sập hoàn toàn
      return BigDecimal.ONE;
   }

   private int getScaleForCurrency(String currencyCode) {
      return "VND".equalsIgnoreCase(currencyCode) ? 0 : 2;
   }

}
