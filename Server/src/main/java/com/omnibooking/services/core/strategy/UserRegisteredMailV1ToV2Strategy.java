package com.omnibooking.services.core.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.omnibooking.services.core.EventUpcasterStrategy;
import org.springframework.stereotype.Component;

/**
 * Migration strategy to upgrade USER_REGISTERED_MAIL events from version 1 to 2.
 * Renames 'content' field to 'htmlContent' and adds a default 'tenantId' if missing.
 */
@Component
public class UserRegisteredMailV1ToV2Strategy implements EventUpcasterStrategy {

   @Override
   public boolean canUpcast(String eventType, int sourceVersion) {
      return "USER_REGISTERED_MAIL".equals(eventType) && sourceVersion == 1;
   }

   @Override
   public JsonNode upcast(JsonNode payload) {
      if (payload instanceof ObjectNode objectNode) {
         if (objectNode.has("content") && !objectNode.has("htmlContent")) {
            objectNode.set("htmlContent", objectNode.get("content"));
         }
         if (!objectNode.has("tenantId")) {
            objectNode.put("tenantId", "default");
         }
      }
      return payload;
   }

}
