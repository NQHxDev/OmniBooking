package com.omnibooking.repository.pricing;

import com.omnibooking.model.PriceRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceRuleVersionRepository extends JpaRepository<PriceRuleVersion, UUID> {

   Optional<PriceRuleVersion> findByPriceRuleIdAndVersion(UUID priceRuleId, Integer version);

   @Query("SELECT COALESCE(MAX(prv.version), 0) FROM PriceRuleVersion prv WHERE prv.priceRule.id = :priceRuleId")
   Integer findMaxVersionByPriceRuleId(@Param("priceRuleId") UUID priceRuleId);

}
