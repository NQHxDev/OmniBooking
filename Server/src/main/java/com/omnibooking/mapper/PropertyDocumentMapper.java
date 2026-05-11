package com.omnibooking.mapper;

import com.omnibooking.document.PropertyDocument;
import com.omnibooking.model.Amenity;
import com.omnibooking.model.Property;
import com.omnibooking.model.RoomType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface PropertyDocumentMapper {

   @Mapping(target = "location", source = "property", qualifiedByName = "mapToGeoPoint")
   @Mapping(target = "amenities", source = "amenities", qualifiedByName = "mapAmenities")
   @Mapping(target = "minPrice", source = "roomTypes", qualifiedByName = "mapMinPrice")
   @Mapping(target = "averageRating", constant = "0.0")
   @Mapping(target = "reviewCount", constant = "0")
   @Mapping(target = "mainImageUrl", ignore = true) // Will be handled in service
   PropertyDocument toDocument(Property property);

   @Named("mapToGeoPoint")
   default GeoPoint mapToGeoPoint(Property property) {
      if (property.getLatitude() == null || property.getLongitude() == null) {
         return null;
      }
      return new GeoPoint(property.getLatitude().doubleValue(), property.getLongitude().doubleValue());
   }

   @Named("mapAmenities")
   default List<String> mapAmenities(Set<Amenity> amenities) {
      if (amenities == null) return Collections.emptyList();
      return amenities.stream().map(Amenity::getName).toList();
   }

   @Named("mapMinPrice")
   default Double mapMinPrice(Set<RoomType> roomTypes) {
      if (roomTypes == null || roomTypes.isEmpty()) return 0.0;
      return roomTypes.stream()
            .map(RoomType::getBasePrice)
            .min(BigDecimal::compareTo)
            .map(BigDecimal::doubleValue)
            .orElse(0.0);
   }
}
