package com.omnibooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;

@SpringBootTest
class ServerApplicationTests {

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

	@Test
	void contextLoads() {
	}

}
