package com.omnibooking.services.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface defining an event schema migration strategy (upcaster).
 * Implementations are dynamically scanned and run sequentially to upgrade event formats.
 */
public interface EventUpcasterStrategy {

   /**
    * Returns true if this strategy is applicable to migrate the eventType from sourceVersion.
    */
   boolean canUpcast(String eventType, int sourceVersion);

   /**
    * Performs the upcast transformation on the payload JSON node.
    * 
    * @param payload the original JSON payload
    * @return the migrated JSON payload
    */
   JsonNode upcast(JsonNode payload);

}
