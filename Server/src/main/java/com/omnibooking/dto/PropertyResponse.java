package com.omnibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class PropertyResponse implements Serializable {

   private static final long serialVersionUID = 1L;

   private UUID id;

   private String name;

   private String propertyType;

   private String city;

   private String country;

   private String imageUrl;

   private java.math.BigDecimal averageRating;

   private Integer reviewCount;

}
