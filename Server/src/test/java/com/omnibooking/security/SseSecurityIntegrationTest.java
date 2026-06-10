package com.omnibooking.security;

import com.omnibooking.services.media.MediaProgressService;
import com.omnibooking.services.media.SseProgressDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.omnibooking.repository.elasticsearch.PropertyElasticsearchRepository;
import com.omnibooking.repository.elasticsearch.DestinationElasticsearchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.mock.web.MockAsyncContext;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.DispatcherType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("all")
public class SseSecurityIntegrationTest {

   private MockMvc mockMvc;

   @Autowired
   private WebApplicationContext wac;

   @MockitoBean
   private MediaProgressService progressService;

   @MockitoBean
   private SseProgressDispatcher sseDispatcher;

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

   private org.springframework.web.servlet.mvc.method.annotation.SseEmitter capturedEmitter;

   @BeforeEach
   public void setup() {
      this.mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .addFilter(new ForwardedHeaderFilter())
            .apply(springSecurity())
            .build();

      this.capturedEmitter = null;
      Mockito.doAnswer(invocation -> {
         this.capturedEmitter = invocation.getArgument(1);
         return null;
      }).when(sseDispatcher).register(any(), any());
   }

   @Test
   public void shouldAllowAsyncDispatchForAuthenticatedPartnerOnSseStream() throws Exception {
      UUID propertyId = UUID.randomUUID();

      // Mock ownership check to true
      Mockito.when(progressService.verifyOwnership((UUID) any(), (UUID) any())).thenReturn(true);

      // Perform initial REQUEST dispatch
      MvcResult mvcResult = mockMvc.perform(get("/media/progress/" + propertyId + "/stream")
            .with(user("partnerUser").authorities(() -> "ROLE_PARTNER")))
            .andExpect(request().asyncStarted())
            .andReturn();

      // Complete the emitter to avoid blocking indefinitely
      if (capturedEmitter != null) {
         capturedEmitter.complete();
      }

      // Dispatch async context - should succeed with status 200 (since
      // DispatcherType.ASYNC is permitted)
      mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());
   }

   @Test
   public void shouldDenyAccessToAnonymousUserOnSseStream() throws Exception {
      UUID propertyId = UUID.randomUUID();

      // Perform standard REQUEST anonymously - should be blocked by security
      // (401/403)
      mockMvc.perform(get("/media/progress/" + propertyId + "/stream"))
            .andExpect(status().is4xxClientError());
   }

   @Test
   public void shouldAllowErrorDispatchAnonymously() throws Exception {
      // Perform ERROR dispatch type - should bypass security filters and not throw
      // 401/403
      // A status of 404 is expected here since /error page controller isn't mapped in
      // mock context,
      // but it validates that the security filter chain did not reject it with 401 or
      // 403.
      mockMvc.perform(get("/error")
            .with(request -> {
               request.setDispatcherType(DispatcherType.ERROR);
               request.setAttribute("jakarta.servlet.error.status_code", 404);
               return request;
            }))
            .andExpect(status().isNotFound());
   }

   @Test
   public void asyncTimeoutDoesNotTriggerAuthorizationDeniedException() throws Exception {
      UUID propertyId = UUID.randomUUID();
      Mockito.when(progressService.verifyOwnership((UUID) any(), (UUID) any())).thenReturn(true);

      // Perform initial REQUEST dispatch
      MvcResult mvcResult = mockMvc.perform(get("/media/progress/" + propertyId + "/stream")
            .with(user("partnerUser").authorities(() -> "ROLE_PARTNER")))
            .andExpect(request().asyncStarted())
            .andReturn();

      // Retrieve the MockAsyncContext
      MockAsyncContext asyncContext = (MockAsyncContext) mvcResult.getRequest().getAsyncContext();

      jakarta.servlet.AsyncEvent event = new jakarta.servlet.AsyncEvent(asyncContext);
      // Trigger timeout listeners
      for (AsyncListener listener : asyncContext.getListeners()) {
         listener.onTimeout(event);
      }

      // Complete the emitter to avoid blocking indefinitely
      if (capturedEmitter != null) {
         capturedEmitter.complete();
      }

      // Perform the async dispatch which completes the request under timeout
      // conditions.
      // This should complete cleanly.
      mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());
   }

   @Test
   public void sseFailureOrClientDisconnectDoesNotTriggerSecurityException() throws Exception {
      UUID propertyId = UUID.randomUUID();
      Mockito.when(progressService.verifyOwnership((UUID) any(), (UUID) any())).thenReturn(true);

      // Perform initial REQUEST dispatch
      MvcResult mvcResult = mockMvc.perform(get("/media/progress/" + propertyId + "/stream")
            .with(user("partnerUser").authorities(() -> "ROLE_PARTNER")))
            .andExpect(request().asyncStarted())
            .andReturn();

      // Retrieve the MockAsyncContext
      MockAsyncContext asyncContext = (MockAsyncContext) mvcResult.getRequest().getAsyncContext();

      jakarta.servlet.AsyncEvent event = new jakarta.servlet.AsyncEvent(asyncContext, new java.io.IOException("Client disconnect"));
      // Simulate async error path (client disconnect or emitter failure)
      for (AsyncListener listener : asyncContext.getListeners()) {
         listener.onError(event);
      }

      // Complete the emitter to avoid blocking indefinitely
      if (capturedEmitter != null) {
         capturedEmitter.complete();
      }

      // Perform async dispatch under error conditions
      mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());
   }

}
