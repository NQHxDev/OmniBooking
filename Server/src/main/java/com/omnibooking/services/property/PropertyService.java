package com.omnibooking.services.property;

import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.dto.PropertyDetailResponse;
import java.util.List;
import java.util.UUID;

public interface PropertyService {

   PropertyResponse createProperty(PropertyRequest request, UUID ownerId);

   List<PropertyResponse> getPropertiesByOwner(UUID ownerId);

    List<PropertyResponse> getFeaturedProperties(int limit);

    List<PropertyResponse> getNewProperties(int limit);

   List<com.omnibooking.dto.PartnerLegalProfileResponse> getPartnerLegalProfiles(UUID partnerId);

   PropertyDetailResponse getPropertyDetailForPartner(UUID propertyId, UUID ownerId);

}
