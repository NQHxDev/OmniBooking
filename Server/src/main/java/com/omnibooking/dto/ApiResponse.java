package com.omnibooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.ZonedDateTime;
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

   private String requestId;

   @Builder.Default
   private ZonedDateTime timestamp = ZonedDateTime.now();

   public static <T> ApiResponse<T> success(T data, String requestId) {
      return ApiResponse.<T>builder()
            .status("success")
            .message("Operation completed successfully")
            .data(data)
            .requestId(requestId)
            .build();
   }

   public static <T> ApiResponse<T> error(String message, Object errors, String requestId) {
      return ApiResponse.<T>builder()
            .status("error")
            .message(message)
            .errors(errors)
            .requestId(requestId)
            .build();
   }

}
