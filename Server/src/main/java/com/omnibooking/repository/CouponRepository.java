package com.omnibooking.repository;

import com.omnibooking.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
   Optional<Coupon> findByCodeIgnoreCase(String code);
}
