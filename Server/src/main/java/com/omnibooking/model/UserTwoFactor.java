package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_two_factor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserTwoFactor extends BaseEntity {

   @OneToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", nullable = false, unique = true)
   private User user;

   @Column(name = "secret_key", nullable = false)
   private String secretKey;

   @Builder.Default
   @Column(name = "is_enabled")
   private Boolean isEnabled = false;

   @Column(name = "backup_codes", columnDefinition = "TEXT")
   private String backupCodes;

}
