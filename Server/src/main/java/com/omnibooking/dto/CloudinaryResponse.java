package com.omnibooking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudinaryResponse(
   @JsonProperty("public_id")
   String publicId,
   
   @JsonProperty("url")
   String url,
   
   @JsonProperty("secure_url")
   String secureUrl,
   
   @JsonProperty("format")
   String format,
   
   @JsonProperty("resource_type")
   String resourceType,
   
   @JsonProperty("bytes")
   Long bytes
) {}
