package com.omnibooking.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleUserInfo implements OAuth2UserInfo {

   @JsonProperty("sub")
   private String id;

   private String email;

   @JsonProperty("verified_email")
   private Boolean verifiedEmail;

   private String name;

   @JsonProperty("given_name")
   private String givenName;

   @JsonProperty("family_name")
   private String familyName;

   private String picture;

   private String locale;

   @Override
   public String getFirstName() {
      return givenName;
   }

   @Override
   public String getLastName() {
      return familyName;
   }

   @Override
   public String getPicture() {
      return picture;
   }
}
