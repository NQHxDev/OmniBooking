package com.omnibooking.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {

   private List<T> items;
   private int currentPage;
   private int totalPages;
   private long totalElements;
   private boolean hasNext;
   private boolean hasPrevious;

   public static <T> PageResponse<T> of(List<T> items, int currentPage, int totalPages, long totalElements) {
      return PageResponse.<T>builder()
            .items(items)
            .currentPage(currentPage)
            .totalPages(totalPages)
            .totalElements(totalElements)
            .hasNext(currentPage < totalPages - 1)
            .hasPrevious(currentPage > 0)
            .build();
   }
}
