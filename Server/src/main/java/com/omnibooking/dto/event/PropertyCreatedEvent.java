package com.omnibooking.dto.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.UUID;

@Getter
public class PropertyCreatedEvent extends ApplicationEvent {

   private final UUID propertyId;

   private final List<UUID> roomTypeIds;

   public PropertyCreatedEvent(Object source, UUID propertyId, List<UUID> roomTypeIds) {
      super(source);
      this.propertyId = propertyId;
      this.roomTypeIds = roomTypeIds;
   }

}
