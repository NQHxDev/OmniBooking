package com.omnibooking.repository;

import com.omnibooking.model.SocialAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

   Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

}
