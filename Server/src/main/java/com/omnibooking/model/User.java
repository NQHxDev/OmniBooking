package com.omnibooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

   @Column(nullable = false, unique = true, length = 50)
   private String username;

   @Column(nullable = false, unique = true, length = 100)
   private String email;

   @Column(nullable = false)
   private String password;

   @Builder.Default
   @Column(name = "is_active")
   private Boolean isActive = true;

   @ManyToMany(fetch = FetchType.EAGER)
   @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
   private Set<Role> roles;

   @OneToOne(mappedBy = "user")
   private UserProfile profile;

}
