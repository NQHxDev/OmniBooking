package com.omnibooking.repository.elasticsearch;

import com.omnibooking.document.PropertyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyElasticsearchRepository extends ElasticsearchRepository<PropertyDocument, String> {
}
