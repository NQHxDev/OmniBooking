package com.omnibooking.repository.user;

import com.omnibooking.model.UserProfile;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

   Optional<UserProfile> findByUserId(UUID userId);

   Optional<UserProfile> findByPhoneSearchHash(String phoneSearchHash);

}
