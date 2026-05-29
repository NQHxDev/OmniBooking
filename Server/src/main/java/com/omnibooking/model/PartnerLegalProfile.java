package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "partner_legal_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PartnerLegalProfile extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "partner_id", nullable = false)
   private User partner;

   @Column(name = "business_registration_number", nullable = false, length = 255)
   private String businessRegistrationNumber;

   @Column(name = "tax_code", nullable = false, length = 255)
   private String taxCode;

   @Column(name = "legal_owner_name", nullable = false, length = 255)
   private String legalOwnerName;

   @Builder.Default
   @Column(name = "is_active", nullable = false)
   private Boolean isActive = true;

}
