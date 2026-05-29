package com.omnibooking.services.user;

import com.omnibooking.dto.RegisterRequest;

public interface RegistrationQueueService {

   void pushToQueue(RegisterRequest request);

}
