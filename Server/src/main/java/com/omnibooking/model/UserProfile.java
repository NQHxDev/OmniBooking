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

   @Column(name = "first_name", length = 50)
   private String firstName;

   @Column(name = "last_name", length = 50)
   private String lastName;

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
