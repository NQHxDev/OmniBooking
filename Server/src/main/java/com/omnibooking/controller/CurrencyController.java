package com.omnibooking.controller;

import com.omnibooking.model.Currency;
import com.omnibooking.repository.CurrencyRepository;
import com.omnibooking.services.CurrencyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/currencies")
@RequiredArgsConstructor
public class CurrencyController {

   private final CurrencyRepository currencyRepository;
   private final CurrencyService currencyService;

   @GetMapping
   public List<Currency> getSupportedCurrencies() {
      return currencyRepository.findAll();
   }

   @GetMapping("/rates")
   public Map<String, BigDecimal> getCurrentRates() {
      List<Currency> currencies = currencyRepository.findAll();
      return currencies.stream()
            .collect(Collectors.toMap(
                  Currency::getCode,
                  c -> currencyService.getRate(c.getCode())
            ));
   }
}
