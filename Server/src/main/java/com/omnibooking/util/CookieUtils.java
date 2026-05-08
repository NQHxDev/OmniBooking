package com.omnibooking.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

public class CookieUtils {

   public static final String ACCESS_TOKEN = "access_token";
   public static final String SESSION_ID = "session_id";
   public static final String REFRESH_TOKEN = "refresh_token";
   public static final String FINGERPRINT = "x_fgp";

   /**
    * Sets auth cookies in the response.
    */
   public static void setAuthCookies(HttpServletResponse response, String accessToken, String sessionId,
         String refreshToken, String fingerprint, boolean secure) {
      addCookie(response, ACCESS_TOKEN, accessToken, 15 * 60, secure); // 15 mins
      addCookie(response, SESSION_ID, sessionId, 7 * 24 * 60 * 60, secure); // 7 days
      addCookie(response, REFRESH_TOKEN, refreshToken, 7 * 24 * 60 * 60, secure); // 7 days
      addCookie(response, FINGERPRINT, fingerprint, 7 * 24 * 60 * 60, secure); // 7 days
   }

   /**
    * Clears all auth cookies.
    */
   public static void clearAuthCookies(HttpServletResponse response, boolean secure) {
      deleteCookie(response, ACCESS_TOKEN, secure);
      deleteCookie(response, SESSION_ID, secure);
      deleteCookie(response, REFRESH_TOKEN, secure);
      deleteCookie(response, FINGERPRINT, secure);
      deleteCookie(response, "csrf_token", secure);
   }

   public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean secure) {
      Cookie cookie = new Cookie(name, value);
      cookie.setHttpOnly(true);
      cookie.setSecure(secure);
      cookie.setPath("/");
      cookie.setMaxAge(maxAge);
      response.addCookie(cookie);
   }

   public static void deleteCookie(HttpServletResponse response, String name, boolean secure) {
      Cookie cookie = new Cookie(name, "");
      cookie.setHttpOnly(true);
      cookie.setSecure(secure);
      cookie.setPath("/");
      cookie.setMaxAge(0);
      response.addCookie(cookie);
   }

   public static String getCookieValue(HttpServletRequest request, String name) {
      if (request.getCookies() == null)
         return null;
      return Arrays.stream(request.getCookies())
            .filter(cookie -> name.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
   }
}
