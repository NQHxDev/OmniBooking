package com.omnibooking.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

   private UUID id;

   private String username;

   private String email;

   private String fullName;

   private String avatarUrl;

   private List<String> roles;

   private Double reputationScore;

   private Boolean isVerified;

   private String rankName;

   private String partnerBio;
   
   private String accessToken;

}
