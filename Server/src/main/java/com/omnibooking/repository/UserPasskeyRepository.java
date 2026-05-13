package com.omnibooking.repository;

import com.omnibooking.model.UserPasskey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPasskeyRepository extends JpaRepository<UserPasskey, UUID> {

   List<UserPasskey> findAllByUserId(UUID userId);

   Optional<UserPasskey> findByCredentialId(String credentialId);

   boolean existsByUserId(UUID userId);

}
