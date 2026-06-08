package com.omnibooking.services.core.impl;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.omnibooking.config.AppProperties;
import com.omnibooking.services.core.GeoLocationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoLocationServiceImpl implements GeoLocationService {

   private final AppProperties appProperties;

   private DatabaseReader databaseReader;

   @PostConstruct
   public void init() {
      try {
         String dbPath = appProperties.getGeo().getDbPath();
         ClassPathResource resource = new ClassPathResource(dbPath);

         if (resource.exists()) {
            try (InputStream inputStream = resource.getInputStream()) {
               databaseReader = new DatabaseReader.Builder(inputStream).build();
               log.info("Successfully initialized GeoIP2 database from: {}", dbPath);
            }
         } else {
            log.warn("GeoIP2 database not found at {}. Geo-location features will use default country: {}",
                  dbPath, appProperties.getGeo().getDefaultCountry());
         }
      } catch (IOException e) {
         log.error("Error initializing GeoIP2 database", e);
      }
   }

   @Override
   public String getCountryCode(String ipAddress) {
      if (databaseReader == null || ipAddress == null || ipAddress.equals("127.0.0.1")
            || ipAddress.equals("0:0:0:0:0:0:0:1")) {
         return appProperties.getGeo().getDefaultCountry();
      }

      try {
         InetAddress inetAddress = InetAddress.getByName(ipAddress);
         CityResponse response = databaseReader.city(inetAddress);
         if (response != null && response.getCountry() != null) {
            return response.getCountry().getIsoCode();
         }
      } catch (IOException | GeoIp2Exception e) {
         log.debug("Could not determine country for IP: {}. Error: {}", ipAddress, e.getMessage());
      }

      return appProperties.getGeo().getDefaultCountry();
   }

   @PreDestroy
   public void preDestroy() {
      if (databaseReader != null) {
         try {
            databaseReader.close();
         } catch (IOException e) {
            log.error("Error closing GeoIP2 database reader", e);
         }
      }
   }

}
