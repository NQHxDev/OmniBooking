package com.omnibooking.services.booking.impl;

import com.omnibooking.model.Property;
import com.omnibooking.model.RoomAvailability;
import com.omnibooking.model.RoomType;
import com.omnibooking.model.User;
import com.omnibooking.model.enums.PropertyType;
import com.omnibooking.repository.property.PropertyRepository;
import com.omnibooking.repository.property.RoomAvailabilityRepository;
import com.omnibooking.repository.property.RoomTypeRepository;
import com.omnibooking.repository.user.UserRepository;
import com.omnibooking.repository.booking.BookingRepository;
import com.omnibooking.repository.booking.BookingStatusLogRepository;
import com.omnibooking.repository.booking.CouponRepository;
import com.omnibooking.repository.booking.InventoryOperationRepository;
import com.omnibooking.repository.property.ReviewRepository;
import com.omnibooking.repository.pricing.BookingAppliedRuleVersionRepository;
import com.omnibooking.repository.pricing.BookingPriceBreakdownRepository;
import com.omnibooking.repository.pricing.CouponReservationRepository;
import com.omnibooking.repository.infra.OutboxEventRepository;
import com.omnibooking.repository.payment.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryLockingBenchmarkTest {

   @Autowired
   private RoomTypeRepository roomTypeRepository;

   @Autowired
   private PropertyRepository propertyRepository;

   @Autowired
   private RoomAvailabilityRepository roomAvailabilityRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private ReviewRepository reviewRepository;

   @Autowired
   private CouponReservationRepository couponReservationRepository;

   @Autowired
   private BookingAppliedRuleVersionRepository bookingAppliedRuleVersionRepository;

   @Autowired
   private BookingPriceBreakdownRepository bookingPriceBreakdownRepository;

   @Autowired
   private BookingStatusLogRepository bookingStatusLogRepository;

   @Autowired
   private InventoryOperationRepository inventoryOperationRepository;

   @Autowired
   private TransactionRepository transactionRepository;

   @Autowired
   private OutboxEventRepository outboxEventRepository;

   @Autowired
   private BookingRepository bookingRepository;

   @Autowired
   private CouponRepository couponRepository;

   @Autowired
   private PlatformTransactionManager transactionManager;

   @MockitoBean
   private ElasticsearchOperations elasticsearchOperations;

   @MockitoBean
   private PropertyElasticsearchRepository propertyElasticsearchRepository;

   @MockitoBean
   private DestinationElasticsearchRepository destinationElasticsearchRepository;

   @MockitoBean
   private KafkaTemplate<String, Object> kafkaTemplate;

   @MockitoBean
   private KafkaAdmin kafkaAdmin;

   @MockitoBean
   private StringRedisTemplate stringRedisTemplate;

   @MockitoBean
   private RedisMessageListenerContainer redisMessageListenerContainer;

   private Property testProperty;

   private RoomType testRoomType;

   private User testUser;

   private LocalDate testDate;

   @BeforeEach
   public void setUp() {
      testDate = LocalDate.now().plusDays(10);

      // Clean old data to avoid unique conflicts
      reviewRepository.deleteAll();
      couponReservationRepository.deleteAll();
      bookingAppliedRuleVersionRepository.deleteAll();
      bookingPriceBreakdownRepository.deleteAll();
      bookingStatusLogRepository.deleteAll();
      inventoryOperationRepository.deleteAll();
      transactionRepository.deleteAll();
      outboxEventRepository.deleteAll();
      bookingRepository.deleteAll();
      roomAvailabilityRepository.deleteAll();
      roomTypeRepository.deleteAll();
      couponRepository.deleteAll();
      propertyRepository.deleteAll();
      userRepository.deleteAll();

      String rand = UUID.randomUUID().toString().substring(0, 8);
      testUser = User.builder()
            .username("bench_usr_" + rand)
            .email("benchmark_" + rand + "@example.com")
            .password("password")
            .isActive(true)
            .build();
      testUser = userRepository.save(testUser);

      testProperty = Property.builder()
            .owner(testUser)
            .name("Benchmark Palace")
            .address("123 Benchmark Ave")
            .city("Hanoi")
            .country("Vietnam")
            .propertyType(PropertyType.HOTEL)
            .isActive(true)
            .build();
      testProperty = propertyRepository.save(testProperty);

      testRoomType = RoomType.builder()
            .property(testProperty)
            .name("Benchmark Room")
            .basePrice(BigDecimal.valueOf(100.00))
            .capacityAdults(2)
            .capacityChildren(0)
            .totalRooms(50) // 50 rooms total
            .build();
      testRoomType = roomTypeRepository.save(testRoomType);
   }

   @Test
   public void runLockingBenchmark() throws InterruptedException {
      int concurrentRequests = 40; // 40 concurrent threads booking 1 room each

      // --- Scenario A: Pessimistic Locking ---
      System.out.println("Starting Benchmark: Pessimistic Locking...");
      BenchmarkResult resultA = runBenchmark(concurrentRequests, false);

      // --- Reset availability ---
      setUp();

      // --- Scenario B: Atomic Updates ---
      System.out.println("Starting Benchmark: Atomic Updates...");
      BenchmarkResult resultB = runBenchmark(concurrentRequests, true);

      // --- Print Report ---
      System.out.println("\n====== INVENTORY LOCKING BENCHMARK REPORT ======");
      System.out.println(String.format("%-25s | %-20s | %-20s", "Metric", "Pessimistic Lock (A)", "Atomic Update (B)"));
      System.out.println("--------------------------------------------------------------------------------");
      System.out
            .println(String.format("%-25s | %-20d | %-20d", "Total Requests", concurrentRequests, concurrentRequests));
      System.out.println(
            String.format("%-25s | %-20d | %-20d", "Successful Bookings", resultA.successCount, resultB.successCount));
      System.out.println(
            String.format("%-25s | %-20d | %-20d", "Oversell Incidents", resultA.oversellCount, resultB.oversellCount));
      System.out.println(String.format("%-25s | %-20d | %-20d", "Deadlock/Lock Failures", resultA.deadlockCount,
            resultB.deadlockCount));
      System.out.println(
            String.format("%-25s | %-20.2f | %-20.2f", "Avg Latency (ms)", resultA.avgLatencyMs, resultB.avgLatencyMs));
      System.out.println(String.format("%-25s | %-20.2f | %-20.2f", "Throughput (RPS)", resultA.throughputRps,
            resultB.throughputRps));
      System.out.println("================================================\n");

      assertNotNull(testRoomType.getId());
   }

   private BenchmarkResult runBenchmark(int threadCount, boolean useAtomic) throws InterruptedException {
      // Initialize room availability
      TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
      txTemplate.execute(status -> {
         RoomAvailability init = RoomAvailability.builder()
               .roomType(testRoomType)
               .availabilityDate(testDate)
               .availableCount(15) // Only 15 rooms available (to trigger contention and exhaustion)
               .isClosed(false)
               .build();
         roomAvailabilityRepository.save(init);
         return null;
      });

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(threadCount);

      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failCount = new AtomicInteger(0);
      AtomicInteger deadlockCount = new AtomicInteger(0);
      AtomicLong totalLatencyNs = new AtomicLong(0);

      for (int i = 0; i < threadCount; i++) {
         executor.submit(() -> {
            try {
               startLatch.await(); // Sync start
               long start = System.nanoTime();

               boolean success = txTemplate.execute(status -> {
                  if (useAtomic) {
                     int rows = roomAvailabilityRepository.deductAvailabilityAtomically(testRoomType.getId(), testDate,
                           1);
                     return rows > 0;
                  } else {
                     RoomAvailability availability = roomAvailabilityRepository
                           .findByRoomTypeIdAndAvailabilityDateWithLock(testRoomType.getId(), testDate)
                           .orElseThrow();
                     if (availability.getAvailableCount() >= 1 && !Boolean.TRUE.equals(availability.getIsClosed())) {
                        availability.setAvailableCount(availability.getAvailableCount() - 1);
                        roomAvailabilityRepository.save(availability);
                        return true;
                     }
                     return false;
                  }
               });

               long latency = System.nanoTime() - start;
               totalLatencyNs.addAndGet(latency);

               if (success) {
                  successCount.incrementAndGet();
               } else {
                  failCount.incrementAndGet();
               }
            } catch (Exception e) {
               failCount.incrementAndGet();
               if (e.getMessage() != null && (e.getMessage().contains("deadlock") || e.getMessage().contains("lock")
                     || e.getClass().getSimpleName().contains("Lock"))) {
                  deadlockCount.incrementAndGet();
               }
            } finally {
               doneLatch.countDown();
            }
         });
      }

      long startTime = System.nanoTime();
      startLatch.countDown(); // Go!
      doneLatch.await(); // Wait for all
      long totalDurationNs = System.nanoTime() - startTime;
      executor.shutdown();

      // Read final room count
      int finalCount = txTemplate.execute(status -> roomAvailabilityRepository
            .findByRoomTypeIdAndAvailabilityDate(testRoomType.getId(), testDate)
            .map(RoomAvailability::getAvailableCount)
            .orElse(0));

      // Calculate metrics
      int actualBookings = successCount.get();
      int expectedBookings = 15; // Starting availability
      assertEquals(expectedBookings, actualBookings, "Successful bookings count must match expected availability");
      int oversellCount = finalCount < 0 ? Math.abs(finalCount) : 0;

      double avgLatencyMs = (totalLatencyNs.get() / (double) threadCount) / 1_000_000.0;
      double throughputRps = threadCount / (totalDurationNs / 1_000_000_000.0);

      return new BenchmarkResult(actualBookings, oversellCount, deadlockCount.get(), avgLatencyMs, throughputRps);
   }

   private static class BenchmarkResult {
      final int successCount;
      final int oversellCount;
      final int deadlockCount;
      final double avgLatencyMs;
      final double throughputRps;

      BenchmarkResult(int successCount, int oversellCount, int deadlockCount, double avgLatencyMs,
            double throughputRps) {
         this.successCount = successCount;
         this.oversellCount = oversellCount;
         this.deadlockCount = deadlockCount;
         this.avgLatencyMs = avgLatencyMs;
         this.throughputRps = throughputRps;
      }
   }

}
