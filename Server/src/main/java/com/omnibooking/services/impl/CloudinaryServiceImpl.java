package com.omnibooking.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibooking.dto.CloudinaryResponse;
import com.omnibooking.services.CloudinaryService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

   private final Cloudinary cloudinary;
   private final ObjectMapper objectMapper;

   @Override
   public CloudinaryResponse upload(MultipartFile file, String folder) throws IOException {
      return upload(file.getBytes(), folder);
   }

   @Override
   @CircuitBreaker(name = "externalService")
   @Retry(name = "externalService")
   public CloudinaryResponse upload(byte[] fileBytes, String folder) throws IOException {
      log.info("Uploading bytes to Cloudinary folder: {}", folder);
      Map<?, ?> rawResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "auto"));

      return objectMapper.convertValue(rawResult, CloudinaryResponse.class);
   }

   @Override
   @SuppressWarnings("unchecked")
   @CircuitBreaker(name = "externalService")
   @Retry(name = "externalService")
   public Map<String, Object> delete(String publicId) throws IOException {
      log.info("Deleting file from Cloudinary: {}", publicId);
      return (Map<String, Object>) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
   }
}
