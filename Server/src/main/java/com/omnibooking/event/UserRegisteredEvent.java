package com.omnibooking.event;

import com.omnibooking.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserRegisteredEvent extends ApplicationEvent {

   private final User user;
   private final String fullName;

   public UserRegisteredEvent(Object source, User user, String fullName) {
      super(source);
      this.user = user;
      this.fullName = fullName;
   }

}
