package com.omnibooking.repository.payment;

import com.omnibooking.model.ExchangeRate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

   Optional<ExchangeRate> findTopByFromCurrencyAndToCurrencyOrderByFetchedAtDesc(String from, String to);

}
