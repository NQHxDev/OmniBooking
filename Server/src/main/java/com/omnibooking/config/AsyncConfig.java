package com.omnibooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

   @Bean(name = "propertyCreationAsyncExecutor")
   public Executor propertyCreationAsyncExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(5);
      executor.setMaxPoolSize(20);
      executor.setQueueCapacity(100);
      executor.setThreadNamePrefix("property-async-");
      executor.setWaitForTasksToCompleteOnShutdown(true);
      executor.setAwaitTerminationSeconds(30);
      executor.initialize();
      return executor;
   }

}
