package com.omnibooking.repository.pricing;

import com.omnibooking.model.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID>, JpaSpecificationExecutor<PriceRule> {

   List<PriceRule> findByPropertyId(UUID propertyId);

   List<PriceRule> findByPropertyIdAndIsActiveTrue(UUID propertyId);

}
