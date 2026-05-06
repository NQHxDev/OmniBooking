package com.omnibooking.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestContext {

   private String requestId;

   private String userId;

}
