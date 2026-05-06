package com.omnibooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

   private String status;

   private String message;

   private T data;

   private Object errors;

   private String errorCode;

   private String requestId;

   @Builder.Default
   private Instant timestamp = Instant.now();

   public static <T> ApiResponse<T> success(T data, String requestId) {
      return ApiResponse.<T>builder()
            .status("success")
            .message("Operation completed successfully")
            .data(data)
            .requestId(requestId)
            .build();
   }

   public static <T> ApiResponse<T> error(String message, String errorCode, Object errors, String requestId) {
      return ApiResponse.<T>builder()
            .status("error")
            .message(message)
            .errorCode(errorCode)
            .errors(errors)
            .requestId(requestId)
            .build();
   }

}
