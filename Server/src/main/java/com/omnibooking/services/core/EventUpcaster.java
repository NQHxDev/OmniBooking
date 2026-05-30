package com.omnibooking.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventUpcaster {

   /**
    * Upcasts event JSON payload from currentVersion to targetVersion.
    */
   public JsonNode upcast(String eventType, JsonNode payload, int currentVersion, int targetVersion) {
      if (currentVersion >= targetVersion) {
         return payload;
      }

      JsonNode upcasted = payload;
      for (int v = currentVersion; v < targetVersion; v++) {
         upcasted = upcastStep(eventType, upcasted, v, v + 1);
      }
      return upcasted;
   }

   private JsonNode upcastStep(String eventType, JsonNode payload, int sourceVer, int targetVer) {
      log.info("Upcasting event {} from v{} to v{}", eventType, sourceVer, targetVer);
      if (payload instanceof ObjectNode objectNode) {
         // Example upcast rule for USER_REGISTERED_MAIL v1 to v2
         if ("USER_REGISTERED_MAIL".equals(eventType) && sourceVer == 1 && targetVer == 2) {
            if (objectNode.has("content") && !objectNode.has("htmlContent")) {
               objectNode.set("htmlContent", objectNode.get("content"));
            }
            if (!objectNode.has("tenantId")) {
               objectNode.put("tenantId", "default");
            }
         }
      }
      return payload;
   }

}
