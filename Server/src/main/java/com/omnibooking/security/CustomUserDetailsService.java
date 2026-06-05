package com.omnibooking.security;

import com.omnibooking.model.User;
import com.omnibooking.repository.user.UserRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

   private final UserRepository userRepository;

   @Override
   @Transactional(readOnly = true)
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

      return UserPrincipal.create(user);
   }

   @Transactional(readOnly = true)
   public UserDetails loadUserById(String id) {
      if (id == null || id.isBlank()) {
         throw new UsernameNotFoundException("User ID is null or empty");
      }

      UUID uuid = UUID.fromString(id);

      User user = userRepository.findById(Objects.requireNonNull(uuid))
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

      return UserPrincipal.create(user);
   }

}
