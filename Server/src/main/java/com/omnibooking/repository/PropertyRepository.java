package com.omnibooking.repository;

import com.omnibooking.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

   List<Property> findByOwnerId(UUID ownerId);

   @org.springframework.data.jpa.repository.Query("SELECT p FROM Property p WHERE p.isActive = true ORDER BY p.createdAt DESC")
   List<Property> findFeaturedProperties(org.springframework.data.domain.Pageable pageable);

}
