package com.omnibooking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

   public static final String MAIL_TOPIC = "omnibooking-mail-topic";

   @Value("${app.kafka.partitions:3}")
   private int partitions;

   @Value("${app.kafka.replicas:1}")
   private int replicas;

   @Bean
   public NewTopic mailTopic() {
      return TopicBuilder.name(MAIL_TOPIC)
            .partitions(partitions)
            .replicas(replicas)
            .build();
   }

}
