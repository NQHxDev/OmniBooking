package com.omnibooking.repository.payment;

import com.omnibooking.model.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, UUID> {

   Optional<Currency> findByCode(String code);

}
