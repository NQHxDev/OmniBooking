package com.omnibooking.repository.property;

import com.omnibooking.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

import java.util.List;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

   List<RoomType> findByPropertyId(UUID propertyId);

}
