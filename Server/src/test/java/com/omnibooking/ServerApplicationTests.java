package com.omnibooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;

@SpringBootTest
class ServerApplicationTests {

	@MockitoBean
	private ElasticsearchOperations elasticsearchOperations;

	@MockitoBean
	private PropertyElasticsearchRepository propertyElasticsearchRepository;

	@Test
	void contextLoads() {
	}

}
