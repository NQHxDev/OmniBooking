package com.omnibooking.services.auth;

public interface TurnstileService {
    void verifyToken(String token, String remoteIp);
}
