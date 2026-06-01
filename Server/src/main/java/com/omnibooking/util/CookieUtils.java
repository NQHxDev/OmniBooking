package com.omnibooking.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.http.ResponseCookie;

public class CookieUtils {

   public static final String ACCESS_TOKEN = "access_token";
   public static final String SESSION_ID = "session_id";
   public static final String REFRESH_TOKEN = "refresh_token";
   public static final String FINGERPRINT = "x_fgp";

   /**
    * Sets auth cookies in the response.
    */
   public static void setAuthCookies(HttpServletResponse response, String accessToken, String sessionId,
         String refreshToken, String fingerprint, boolean secure, int expiry) {
      
      addCookie(response, ACCESS_TOKEN, accessToken, 15 * 60, secure); // Access token always 15 mins
      addCookie(response, SESSION_ID, sessionId, expiry, secure);
      addCookie(response, REFRESH_TOKEN, refreshToken, expiry, secure);
      addCookie(response, FINGERPRINT, fingerprint, expiry, secure);
      
      // Set csrf_token cookie for double submit cookie verification (not HttpOnly so client JS can read it)
      String csrfToken = java.util.UUID.randomUUID().toString();
      addCookie(response, "csrf_token", csrfToken, expiry, secure);
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
      boolean httpOnly = !name.equals("csrf_token");
      ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(secure)
            .path("/")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build();
      response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
   }

   public static void deleteCookie(HttpServletResponse response, String name, boolean secure) {
      boolean httpOnly = !name.equals("csrf_token");
      ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(httpOnly)
            .secure(secure)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();
      response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
   }

   public static String getCookieValue(HttpServletRequest request, String name) {
      if (request.getCookies() != null) {
         return Arrays.stream(request.getCookies())
               .filter(cookie -> name.equals(cookie.getName()))
               .map(Cookie::getValue)
               .findFirst()
               .orElse(null);
      }

      // Manual parsing if getCookies() is null (common in server-to-server fetches)
      String cookieHeader = request.getHeader("Cookie");
      if (cookieHeader != null) {
         return Arrays.stream(cookieHeader.split(";"))
               .map(String::trim)
               .filter(s -> s.startsWith(name + "="))
               .map(s -> s.substring(name.length() + 1))
               .findFirst()
               .orElse(null);
      }

      return null;
   }

}
