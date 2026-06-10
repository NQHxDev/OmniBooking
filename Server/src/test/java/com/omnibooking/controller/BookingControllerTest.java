package com.omnibooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.BookingResponse;
import com.omnibooking.dto.CreateBookingRequest;
import com.omnibooking.mapper.PropertyDocumentMapper;
import com.omnibooking.mapper.UserMapper;
import com.omnibooking.model.enums.BookingStatus;
import com.omnibooking.services.booking.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import com.omnibooking.util.CookieUtils;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookingControllerTest {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @MockitoBean
   private BookingService bookingService;

   @MockitoBean
   private PropertyDocumentMapper propertyDocumentMapper;

   @MockitoBean
   private UserMapper userMapper;

   // Mock external systems to avoid connection issues in tests
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

   @MockitoBean
   private StringRedisTemplate stringRedisTemplate;

   @MockitoBean
   private RedisMessageListenerContainer redisMessageListenerContainer;

   @MockitoBean
   private ValueOperations<String, String> valueOps;

   @Test
   public void shouldCreateBookingSuccessfully() throws Exception {
      Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
      Mockito.when(valueOps.setIfAbsent(any(), any(), Mockito.anyLong(), any())).thenReturn(true);

      UUID roomTypeId = UUID.randomUUID();
      CreateBookingRequest request = CreateBookingRequest.builder()
            .roomTypeId(roomTypeId)
            .checkInDate(LocalDate.now().plusDays(1))
            .checkOutDate(LocalDate.now().plusDays(5))
            .numRooms(1)
            .guestName("John Doe")
            .guestEmail("john@example.com")
            .guestPhone("0123456789")
            .build();

      BookingResponse response = BookingResponse.builder()
            .id(UUID.randomUUID())
            .bookingCode("ABC12345")
            .guestName("John Doe")
            .guestEmail("john@example.com")
            .propertyName("Grand Hotel")
            .roomTypeName("Deluxe Suite")
            .checkInDate(request.getCheckInDate())
            .checkOutDate(request.getCheckOutDate())
            .numRooms(1)
            .totalPrice(BigDecimal.valueOf(400))
            .finalPrice(BigDecimal.valueOf(400))
            .status(BookingStatus.CONFIRMED)
            .build();

      Mockito.when(bookingService.createBooking(any(CreateBookingRequest.class), any())).thenReturn(response);

      mockMvc.perform(post("/bookings")
            .header("Origin", "http://localhost:3000")
            .header("X-Idempotency-Key", UUID.randomUUID().toString())
            .cookie(new Cookie(CookieUtils.CSRF_TOKEN, "test_csrf"))
            .header("X-CSRF-Token", "test_csrf")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Booking created successfully"))
            .andExpect(jsonPath("$.data.bookingCode").value("ABC12345"))
            .andExpect(jsonPath("$.data.guestEmail").value("john@example.com"));
   }

}
