package com.omnibooking.dto.event;

import com.omnibooking.dto.MediaProgress;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Spring ApplicationEvent published after each media progress state change.
 * Consumed by SseProgressDispatcher to push updates to connected SSE clients.
 */
@Getter
public class MediaProgressUpdatedEvent extends ApplicationEvent {

   private final UUID propertyId;

   private final MediaProgress progress;

   public MediaProgressUpdatedEvent(Object source, UUID propertyId, MediaProgress progress) {
      super(source);
      this.propertyId = propertyId;
      this.progress = progress;
   }

}
