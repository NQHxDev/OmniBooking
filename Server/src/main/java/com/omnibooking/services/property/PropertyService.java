package com.omnibooking.services.property;

import com.omnibooking.dto.PropertyRequest;
import com.omnibooking.dto.PropertyResponse;
import com.omnibooking.dto.PropertyDetailResponse;
import com.omnibooking.dto.IncompleteUploadResponse;
import com.omnibooking.dto.PartnerLegalProfileResponse;

import java.util.List;
import java.util.UUID;

public interface PropertyService {

   PropertyResponse createProperty(PropertyRequest request, UUID ownerId);

   List<PropertyResponse> getPropertiesByOwner(UUID ownerId);

   List<PropertyResponse> getFeaturedProperties(int limit);

   List<PropertyResponse> getNewProperties(int limit);

   List<PartnerLegalProfileResponse> getPartnerLegalProfiles(UUID partnerId);

   PropertyDetailResponse getPropertyDetailForPartner(UUID propertyId, UUID ownerId);

   PropertyDetailResponse getPropertyDetail(UUID propertyId);

   void evictPartnerPropertiesCache(UUID ownerId);

   void evictPublicPropertiesCache();

   List<IncompleteUploadResponse> getIncompleteUploads(UUID ownerId);

   void dismissIncompleteUpload(UUID propertyId, UUID ownerId);

}
