package com.omnibooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RegistrationWorkerPoolConfig {

   @Bean(name = "registrationCpuExecutor")
   public Executor registrationCpuExecutor() {
      int cores = Runtime.getRuntime().availableProcessors();
      
      // Use standard platform threads for CPU-heavy Argon2 hashing to avoid Virtual Thread carrier pinning
      ThreadFactory platformThreadFactory = new ThreadFactory() {
         private final AtomicInteger threadNumber = new AtomicInteger(1);
         @Override
         public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "registration-cpu-worker-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
         }
      };

      // Bounded queue size of 500 with CallerRunsPolicy to apply backpressure to the Kafka Consumer Thread
      return new ThreadPoolExecutor(
            cores,
            cores,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500),
            platformThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
      );
   }

}
