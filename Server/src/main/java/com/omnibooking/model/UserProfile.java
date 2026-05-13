package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

   @Id
   private UUID userId;

   @OneToOne
   @MapsId
   @JoinColumn(name = "user_id")
   private User user;

   @Column(name = "display_name", length = 100)
   private String displayName;
 
   @Column(name = "date_of_birth")
   private java.time.LocalDate dateOfBirth;
 
   @Column(name = "gender", length = 20)
   private String gender;
 
   @Column(name = "address")
   private String address;
 
   @Column(name = "nationality", length = 100)
   private String nationality;
 
   @Column(name = "phone_number", unique = true, length = 20)
   private String phoneNumber;

   @Column(name = "avatar_url")
   private String avatarUrl;

   @Builder.Default
   private Integer points = 0;

   @ManyToOne
   @JoinColumn(name = "rank_id")
   private Rank rank;

   @Builder.Default
   @Column(name = "reputation_score")
   private Double reputationScore = 100.0;

   @Builder.Default
   @Column(name = "is_verified")
   private Boolean isVerified = false;

   @Column(name = "partner_bio", columnDefinition = "TEXT")
   private String partnerBio;

   @Version
   private Long version;

   @LastModifiedDate
   @Column(name = "updated_at")
   private Instant updatedAt;

   @Column(name = "deleted_at")
   private Instant deletedAt;

}
