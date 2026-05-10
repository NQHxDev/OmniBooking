package com.omnibooking.mapper;

import com.omnibooking.dto.AuthResponse;
import com.omnibooking.dto.RegisterRequest;
import com.omnibooking.model.User;
import com.omnibooking.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "roles", ignore = true)
   @Mapping(target = "isActive", constant = "true")
   @Mapping(target = "username", source = "email")
   @Mapping(target = "password", ignore = true)
   @Mapping(target = "createdAt", ignore = true)
   @Mapping(target = "updatedAt", ignore = true)
   @Mapping(target = "deletedAt", ignore = true)
   @Mapping(target = "version", ignore = true)
   @Mapping(target = "profile", ignore = true)
   User toUser(RegisterRequest request);

   @Mapping(target = "id", source = "user.id")
   @Mapping(target = "username", source = "user.username")
   @Mapping(target = "email", source = "user.email")
   @Mapping(target = "roles", source = "roles", qualifiedByName = "setToOrderedList")
   @Mapping(target = "fullName", expression = "java(profile != null ? profile.getFirstName() + \" \" + profile.getLastName() : user.getUsername())")
   @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
   @Mapping(target = "reputationScore", expression = "java(profile != null ? profile.getReputationScore() : 100.0)")
   @Mapping(target = "isVerified", expression = "java(profile != null ? profile.getIsVerified() : false)")
   @Mapping(target = "rankName", expression = "java(profile != null && profile.getRank() != null ? profile.getRank().getName() : \"Bronze\")")
   @Mapping(target = "partnerBio", source = "profile.partnerBio")
   AuthResponse toAuthResponse(User user, UserProfile profile, Set<String> roles);

   @Named("setToOrderedList")
   default List<String> setToOrderedList(Set<String> roles) {
      if (roles == null)
         return null;
      return new ArrayList<>(roles);
   }
}
