package com.omnibooking.repository.infra;

import com.omnibooking.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

   List<Media> findByEntityIdAndEntityType(UUID entityId, String entityType);

   long countByEntityIdAndEntityType(UUID entityId, String entityType);

   Optional<Media> findFirstByEntityIdAndEntityTypeAndIsMainTrue(UUID entityId, String entityType);

   @Query("SELECT m FROM Media m WHERE m.entityType = 'PROPERTY' AND m.isMain = true AND m.entityId IN :entityIds")
   List<Media> findMainImagesByEntityIds(@Param("entityIds") List<UUID> entityIds);

   @Modifying
   @Query("DELETE FROM Media m WHERE m.entityType IN :entityTypes")
   void deleteByEntityTypeIn(@Param("entityTypes") List<String> entityTypes);

}
