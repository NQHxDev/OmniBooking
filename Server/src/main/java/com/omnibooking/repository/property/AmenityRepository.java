package com.omnibooking.repository.property;

import com.omnibooking.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

   Optional<Amenity> findByNameIgnoreCase(String name);

}
