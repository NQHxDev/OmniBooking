package com.omnibooking.services;

import com.omnibooking.dto.RegisterRequest;

public interface RegistrationQueueService {

   void pushToQueue(RegisterRequest request);

}
