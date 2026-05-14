package com.omnibooking.repository;

import com.omnibooking.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

   List<Media> findByEntityIdAndEntityType(UUID entityId, String entityType);

   Optional<Media> findFirstByEntityIdAndEntityTypeAndIsMainTrue(UUID entityId, String entityType);

}
