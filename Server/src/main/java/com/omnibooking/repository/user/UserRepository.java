package com.omnibooking.repository.user;

import com.omnibooking.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

   Optional<User> findByUsername(String username);

   Optional<User> findByEmail(String email);

   boolean existsByUsername(String username);

   boolean existsByEmail(String email);
   
   List<User> findByEmailEndingWith(String emailSuffix);

   @Query("SELECT u.id, u.email FROM User u WHERE (:lastId IS NULL OR u.id > :lastId) AND u.deletedAt IS NULL ORDER BY u.id ASC")
   List<Object[]> findEmailsForWarmup(@Param("lastId") UUID lastId, Pageable pageable);

}
