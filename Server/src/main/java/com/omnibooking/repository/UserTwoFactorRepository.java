package com.omnibooking.repository;

import com.omnibooking.model.UserTwoFactor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTwoFactorRepository extends JpaRepository<UserTwoFactor, UUID> {

   Optional<UserTwoFactor> findByUserId(UUID userId);

   Optional<UserTwoFactor> findByUserEmail(String email);

}
