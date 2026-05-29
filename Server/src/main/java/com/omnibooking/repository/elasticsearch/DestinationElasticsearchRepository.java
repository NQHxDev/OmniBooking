package com.omnibooking.repository.elasticsearch;

import com.omnibooking.document.DestinationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.elasticsearch.annotations.Query;
import java.util.List;

@Repository
public interface DestinationElasticsearchRepository extends ElasticsearchRepository<DestinationDocument, String> {

   @Query("{\"bool\": {\"should\": [" +
         "{\"match_phrase_prefix\": {\"name\": \"?0\"}}," +
         "{\"match\": {\"name\": \"?0\"}}" +
         "]}}")
   List<DestinationDocument> searchByName(String name);

   List<DestinationDocument> findTop15ByCountryCodeOrderByPopularityScoreDesc(String countryCode);

   List<DestinationDocument> findTop15ByOrderByPopularityScoreDesc();

}
