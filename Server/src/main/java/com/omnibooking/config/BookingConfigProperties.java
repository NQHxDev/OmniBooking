package com.omnibooking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "omnibooking.booking")
@Getter
@Setter
public class BookingConfigProperties {

   /** Duration a booking stays in PENDING_PAYMENT before expiring */
   private int holdDurationMinutes = 30;

   /** How often the expiration worker runs */
   private long expirationCheckIntervalMs = 60000;

   /** Max bookings processed per expiration worker cycle batch */
   private int expirationBatchSize = 500;

   /** Grace period after check-in time before marking as NO_SHOW.
    *  Supports arbitrary values (e.g. 2, 6, 12, 36).
    *  Used with Instant-based calculation — no integer division. */
   private int noShowGracePeriodHours = 24;

}
