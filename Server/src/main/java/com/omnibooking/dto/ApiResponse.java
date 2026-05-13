package com.omnibooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

   private String message;

   private String errorCode;

   private T data;

   private String requestId;

   @Builder.Default
   private String timestamp = Instant.now().toString();

   public static <T> ApiResponse<T> success(T data) {
      return ApiResponse.<T>builder()
            .message("Success")
            .data(data)
            .build();
   }

   public static <T> ApiResponse<T> success(T data, String message, String requestId) {
      return ApiResponse.<T>builder()
            .message(message)
            .data(data)
            .requestId(requestId)
            .build();
   }

   public static <T> ApiResponse<T> error(String message, String errorCode, T data, String requestId) {
      return ApiResponse.<T>builder()
            .message(message)
            .errorCode(errorCode)
            .data(data)
            .requestId(requestId)
            .build();
   }

   public static <T> ApiResponse<T> error(String message, String errorCode, String requestId) {
      return ApiResponse.<T>builder()
            .message(message)
            .errorCode(errorCode)
            .requestId(requestId)
            .build();
   }

}
