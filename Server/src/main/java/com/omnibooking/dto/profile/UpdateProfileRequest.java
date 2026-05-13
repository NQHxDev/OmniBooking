package com.omnibooking.dto.profile;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

   private String displayName;

   private LocalDate dateOfBirth;

   private String gender;

   private String address;

   private String nationality;

   private String avatarUrl;

}
