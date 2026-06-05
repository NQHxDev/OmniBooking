package com.omnibooking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

   public static final String MAIL_TOPIC = "omnibooking-mail-topic";
   public static final String MEDIA_TOPIC = "omnibooking-media-topic";
   public static final String PROPERTY_SYNC_TOPIC = "omnibooking-property-sync";
   public static final String DEFAULT_TOPIC = "omnibooking-default-topic";

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

   @Bean
   public NewTopic mediaTopic() {
      return TopicBuilder.name(MEDIA_TOPIC)
            .partitions(partitions)
            .replicas(replicas)
            .build();
   }

   @Bean
   public NewTopic propertySyncTopic() {
      return TopicBuilder.name(PROPERTY_SYNC_TOPIC)
            .partitions(partitions)
            .replicas(replicas)
            .build();
   }

   @Bean
   public NewTopic defaultTopic() {
      return TopicBuilder.name(DEFAULT_TOPIC)
            .partitions(partitions)
            .replicas(replicas)
            .build();
   }

   @Value("${omnibooking.kafka.registration.topic-name:registration-request-topic}")
   private String registrationTopicName;

   @Value("${omnibooking.kafka.registration.partitions:16}")
   private int registrationPartitions;

   @Value("${omnibooking.kafka.registration.replications:1}")
   private int registrationReplications;

   @Bean
   public NewTopic registrationTopic() {
      return TopicBuilder.name(registrationTopicName)
            .partitions(registrationPartitions)
            .replicas(registrationReplications)
            .build();
   }

   @Bean
   public NewTopic registrationDltTopic() {
      return TopicBuilder.name(registrationTopicName + "-dlt")
            .partitions(registrationPartitions)
            .replicas(registrationReplications)
            .build();
   }

   @Bean("registrationListenerContainerFactory")
   public ConcurrentKafkaListenerContainerFactory<String, Object> registrationListenerContainerFactory(
         ConsumerFactory<String, Object> consumerFactory,
         KafkaTemplate<String, Object> kafkaTemplate) {
      
      ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
      factory.setConsumerFactory(consumerFactory);
      factory.setBatchListener(true);
      factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
      
      // Configure DLT (Dead Letter Topic) recovery handler with 3 retries (1s, 2s, 4s backoff)
      DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (r, e) -> new TopicPartition(r.topic() + "-dlt", r.partition()));
      
      DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
      factory.setCommonErrorHandler(errorHandler);
      
      return factory;
   }

}
