package com.omnibooking.repository.property;

import com.omnibooking.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

   Optional<Amenity> findByNameIgnoreCase(String name);

   @Query("SELECT a FROM Amenity a WHERE LOWER(a.name) IN :names")
   List<Amenity> findByNameInIgnoreCase(@Param("names") Collection<String> names);

}
