package com.omnibooking.services;

import com.omnibooking.document.DestinationDocument;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.services.property.impl.DestinationServiceImpl;
import com.omnibooking.services.search.SearchLogService;
import com.omnibooking.services.search.TrendingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DestinationServiceTest {

   @Mock
   private DestinationElasticsearchRepository destinationRepository;

   @Mock
   private SearchLogService searchLogService;

   @Mock
   private TrendingService trendingService;

   @Mock
   private PropertyElasticsearchRepository propertyElasticsearchRepository;

   @InjectMocks
   private DestinationServiceImpl destinationService;

   @Test
   void testRegisterDestinationIfNeeded_WhenCityExists_ShouldNotRegister() {
      // Arrange
      String cityName = "Đồng Tháp";
      DestinationDocument existingDest = DestinationDocument.builder()
            .id("existing-id")
            .name("Đồng Tháp")
            .build();

      when(destinationRepository.searchByName(cityName)).thenReturn(List.of(existingDest));

      // Act
      destinationService.registerDestinationIfNeeded(cityName, "Việt Nam", 10.5, 105.5);

      // Assert
      verify(destinationRepository, never()).save(any(DestinationDocument.class));
   }

   @Test
   void testRegisterDestinationIfNeeded_WhenCityDoesNotExist_ShouldRegister() {
      // Arrange
      String cityName = "Đồng Tháp";
      when(destinationRepository.searchByName(cityName)).thenReturn(Collections.emptyList());

      // Act
      destinationService.registerDestinationIfNeeded(cityName, "Việt Nam", 10.5, 105.5);

      // Assert
      ArgumentCaptor<DestinationDocument> captor = ArgumentCaptor.forClass(DestinationDocument.class);
      verify(destinationRepository, times(1)).save(captor.capture());

      DestinationDocument saved = captor.getValue();
      assertNotNull(saved.getId());
      assertEquals("Đồng Tháp", saved.getName());
      assertEquals("CITY", saved.getType());
      assertEquals("VN", saved.getCountryCode());
      assertEquals("Việt Nam", saved.getCountryName());
      assertNotNull(saved.getLocation());
      assertEquals(10.5, saved.getLocation().getLat());
      assertEquals(105.5, saved.getLocation().getLon());
   }
}
