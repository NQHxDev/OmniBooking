package com.omnibooking.services;

import com.omnibooking.dto.oauth.OAuth2UserInfo;

public interface OAuth2ProviderService {

   String getProviderName();

   String generateAuthUrl();

   OAuth2UserInfo exchangeCodeForUserInfo(String code, String state);

}
