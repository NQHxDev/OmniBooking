package com.omnibooking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

   public static final String MAIL_TOPIC = "omnibooking-mail-topic";

   @Bean
   public NewTopic mailTopic() {
      return TopicBuilder.name(MAIL_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
   }

}
