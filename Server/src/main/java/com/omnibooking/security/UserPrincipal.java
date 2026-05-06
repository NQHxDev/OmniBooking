package com.omnibooking.security;

import com.omnibooking.model.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

   private final UUID id;
   private final String username;
   private final String password;
   private final String email;
   private final Collection<? extends GrantedAuthority> authorities;
   private final boolean active;

   public static UserPrincipal create(User user) {
      List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toList());

      return UserPrincipal.builder()
            .id(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .email(user.getEmail())
            .authorities(authorities)
            .active(user.getIsActive())
            .build();
   }

   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
      return authorities;
   }

   @Override
   public String getPassword() {
      return password;
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public boolean isAccountNonExpired() {
      return true;
   }

   @Override
   public boolean isAccountNonLocked() {
      return active;
   }

   @Override
   public boolean isCredentialsNonExpired() {
      return true;
   }

   @Override
   public boolean isEnabled() {
      return active;
   }
}
