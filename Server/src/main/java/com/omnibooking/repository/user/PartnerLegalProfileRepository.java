package com.omnibooking.repository.user;

import com.omnibooking.model.PartnerLegalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerLegalProfileRepository extends JpaRepository<PartnerLegalProfile, UUID> {

   List<PartnerLegalProfile> findByPartnerId(UUID partnerId);

   List<PartnerLegalProfile> findByPartnerIdAndIsActiveTrueOrderByCreatedAtAsc(UUID partnerId);

   List<PartnerLegalProfile> findByPartnerIdAndIsActiveTrueOrderByCreatedAtDesc(UUID partnerId);

   Optional<PartnerLegalProfile> findByPartnerIdAndProfileSearchHash(UUID partnerId, String profileSearchHash);

}
