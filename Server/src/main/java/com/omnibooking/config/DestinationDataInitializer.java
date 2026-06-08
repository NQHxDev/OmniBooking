package com.omnibooking.config;

import com.omnibooking.document.DestinationDocument;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@Order(4)
@RequiredArgsConstructor
public class DestinationDataInitializer implements CommandLineRunner {

   private final DestinationElasticsearchRepository repository;

   @Override
   public void run(String... args) {
      List<DestinationDocument> destinations = Arrays.asList(
            // Vietnam
            createDest("1", "Hà Nội", "CITY", "VN", "Việt Nam", 21.0285, 105.8542, 100.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780019151/HaNoi_z7ahy3.png"),
            createDest("2", "Thành Phố Hồ Chí Minh", "CITY", "VN", "Việt Nam", 10.8231, 106.6297, 95.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780019498/TP-HCM_z1rn9m.jpg"),
            createDest("3", "Đà Nẵng", "CITY", "VN", "Việt Nam", 16.0544, 108.2022, 90.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780020710/DaNang_s8vktp.png"),
            createDest("4", "Hội An", "CITY", "VN", "Việt Nam", 15.8801, 108.3380, 85.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780022069/HoiAn_r5iwvs.png"),
            createDest("5", "Phú Quốc", "CITY", "VN", "Việt Nam", 10.2899, 103.9840, 88.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780021639/PhuQuoc_qx5m73.png"),
            createDest("11", "Quảng Ninh", "REGION", "VN", "Việt Nam", 20.9599, 107.0425, 82.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780020793/HaLong_hzznrp.jpg"),
            createDest("13", "Nha Trang", "CITY", "VN", "Việt Nam", 12.2388, 109.1967, 87.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780021782/NhaTrang_yzgfxa.jpg"),
            createDest("14", "Đà Lạt", "CITY", "VN", "Việt Nam", 11.9404, 108.4583, 91.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780019811/DaLat_toqzoy.png"),
            createDest("15", "Vũng Tàu", "CITY", "VN", "Việt Nam", 10.3460, 107.0843, 80.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780022719/VungTau_wojsbl.jpg"),
            createDest("16", "Sapa", "CITY", "VN", "Việt Nam", 22.3364, 103.8438, 86.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780021850/Sapa_t5apez.webp"),
            createDest("17", "Huế", "CITY", "VN", "Việt Nam", 16.4637, 107.5909, 84.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780022143/Hue_acx9vq.jpg"),
            createDest("18", "Hải Phòng", "CITY", "VN", "Việt Nam", 20.8449, 106.6881, 78.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780022789/HaiPhong_corqgm.jpg"),
            createDest("19", "Cần Thơ", "CITY", "VN", "Việt Nam", 10.0452, 105.7469, 75.0,
                  "https://res.cloudinary.com/dbm3etf8n/image/upload/v1780022854/CanTho_jgs1ay.jpg"),

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
