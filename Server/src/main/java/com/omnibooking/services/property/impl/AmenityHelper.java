package com.omnibooking.services.property.impl;

import com.omnibooking.model.Amenity;
import com.omnibooking.repository.property.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityHelper {

   private final AmenityRepository amenityRepository;

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public List<Amenity> saveAllInNewTransaction(List<Amenity> amenities) {
      return amenityRepository.saveAll(amenities);
   }

   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public Amenity saveInNewTransaction(Amenity amenity) {
      return amenityRepository.save(amenity);
   }

}
