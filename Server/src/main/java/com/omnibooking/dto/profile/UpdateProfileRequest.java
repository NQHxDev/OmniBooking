package com.omnibooking.dto.profile;

import lombok.Data;
import java.time.LocalDate;
import com.omnibooking.validation.ValidPhoneNumber;

@Data
public class UpdateProfileRequest {

   private String displayName;

   private LocalDate dateOfBirth;

   private String gender;

   private String address;

   private String nationality;

   private String avatarUrl;

   @ValidPhoneNumber
   private String phoneNumber;

}
