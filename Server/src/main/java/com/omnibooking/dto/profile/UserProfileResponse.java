package com.omnibooking.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

   private String email;

   private String displayName;

   private LocalDate dateOfBirth;

   private String gender;

   private String address;

   private String nationality;

   private String phoneNumber;

   private String avatarUrl;

   private boolean isVerified;

   private Integer points;

   private String rankName;

   private boolean hasPassword;

}
