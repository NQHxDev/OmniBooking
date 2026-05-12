package com.omnibooking.config;

import com.omnibooking.document.DestinationDocument;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DestinationDataInitializer implements CommandLineRunner {

   private final DestinationElasticsearchRepository repository;

   @Override
   public void run(String... args) {
      List<DestinationDocument> destinations = Arrays.asList(
            // Vietnam
            createDest("1", "Hà Nội", "CITY", "VN", "Việt Nam", 21.0285, 105.8542, 100.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/hanoi.jpg"),
            createDest("2", "Hồ Chí Minh", "CITY", "VN", "Việt Nam", 10.8231, 106.6297, 95.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/hcmc.jpg"),
            createDest("3", "Đà Nẵng", "CITY", "VN", "Việt Nam", 16.0544, 108.2022, 90.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/danang.jpg"),
            createDest("4", "Hội An", "CITY", "VN", "Việt Nam", 15.8801, 108.3380, 85.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/hoian.jpg"),
            createDest("5", "Phú Quốc", "CITY", "VN", "Việt Nam", 10.2899, 103.9840, 88.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/phuquoc.jpg"),
            createDest("11", "Quảng Ninh", "REGION", "VN", "Việt Nam", 20.9599, 107.0425, 82.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/quangninh.jpg"),
            createDest("12", "Hạ Long", "CITY", "VN", "Việt Nam", 20.9505, 107.0733, 89.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/halong.jpg"),
            createDest("13", "Nha Trang", "CITY", "VN", "Việt Nam", 12.2388, 109.1967, 87.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/nhatrang.jpg"),
            createDest("14", "Đà Lạt", "CITY", "VN", "Việt Nam", 11.9404, 108.4583, 91.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/dalat.jpg"),
            createDest("15", "Vũng Tàu", "CITY", "VN", "Việt Nam", 10.3460, 107.0843, 80.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/vungtau.jpg"),
            createDest("16", "Sapa", "CITY", "VN", "Việt Nam", 22.3364, 103.8438, 86.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/sapa.jpg"),
            createDest("17", "Huế", "CITY", "VN", "Việt Nam", 16.4637, 107.5909, 84.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/hue.jpg"),
            createDest("18", "Hải Phòng", "CITY", "VN", "Việt Nam", 20.8449, 106.6881, 78.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/haiphong.jpg"),
            createDest("19", "Cần Thơ", "CITY", "VN", "Việt Nam", 10.0452, 105.7469, 75.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/cantho.jpg"),

            // International
            createDest("6", "Paris", "CITY", "FR", "France", 48.8566, 2.3522, 98.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/paris.jpg"),
            createDest("7", "London", "CITY", "GB", "United Kingdom", 51.5074, -0.1278, 97.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/london.jpg"),
            createDest("8", "Tokyo", "CITY", "JP", "Japan", 35.6762, 139.6503, 99.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/tokyo.jpg"),
            createDest("9", "New York", "CITY", "US", "United States", 40.7128, -74.0060, 96.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/newyork.jpg"),
            createDest("10", "Bangkok", "CITY", "TH", "Thailand", 13.7563, 100.5018, 92.0,
                  "https://res.cloudinary.com/demo/image/upload/v1/geo/bangkok.jpg"));

      repository.saveAll(destinations);
      log.info("Successfully seeded {} destinations into Elasticsearch.", destinations.size());
   }

   private DestinationDocument createDest(String id, String name, String type, String countryCode, String countryName,
         double lat, double lon, double score, String imageUrl) {
      return DestinationDocument.builder()
            .id(id)
            .name(name)
            .type(type)
            .countryCode(countryCode)
            .countryName(countryName)
            .location(new GeoPoint(lat, lon))
            .popularityScore(score)
            .imageUrl(imageUrl)
            .build();
   }
}
