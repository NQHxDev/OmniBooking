package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_passkeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserPasskey extends BaseEntity {

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(name = "credential_id", nullable = false, unique = true)
   private String credentialId;

   @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
   private String publicKey;

   @Column(name = "sign_count", nullable = false)
   private Long signCount;

   @Column(name = "label")
   private String label;

   @Column(name = "aaguid")
   private String aaguid;

}
