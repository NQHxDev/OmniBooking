package com.omnibooking.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public class CookieUtils {

   public static final String ACCESS_TOKEN = "access_token";

   public static final String SESSION_ID = "session_id";

   public static final String REFRESH_TOKEN = "refresh_token";

   public static final String FINGERPRINT = "x_fgp";

   public static final String CSRF_TOKEN = "csrf_token";

   public static String cookieDomain;

   public static String csrfSecret;

   /**
    * Calculates CSRF Token bound to session using HMAC-SHA256.
    */
   public static String calculateCsrfToken(String sessionId, String secret) {
      return calculateCsrfToken(sessionId, null, secret);
   }

   /**
    * Calculates CSRF Token bound to session and nonce using HMAC-SHA256.
    */
   public static String calculateCsrfToken(String sessionId, String csrfNonce, String secret) {
      if (sessionId == null || sessionId.isBlank() || secret == null || secret.isBlank()) {
         return UUID.randomUUID().toString();
      }
      String input = sessionId + (csrfNonce != null ? csrfNonce : "");
      try {
         Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
         SecretKeySpec secretKeySpec = new SecretKeySpec(
               secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
         sha256_HMAC.init(secretKeySpec);
         byte[] hash = sha256_HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8));
         return HexFormat.of().formatHex(hash);
      } catch (Exception e) {
         return UUID.randomUUID().toString();
      }
   }

   /**
    * Resolves the cookie domain dynamically based on request host.
    */
   private static String resolveCookieDomain() {
      if (cookieDomain != null && !cookieDomain.isBlank()) {
         return cookieDomain;
      }
      return null;
   }

   /**
    * Sets auth cookies in the response.
    */
   public static void setAuthCookies(HttpServletResponse response, String accessToken, String sessionId,
         String refreshToken, String fingerprint, String csrfNonce, boolean secure, int expiry) {

      addCookie(response, ACCESS_TOKEN, accessToken, 15 * 60, secure); // Access token always 15 mins
      addCookie(response, SESSION_ID, sessionId, expiry, secure);
      addCookie(response, REFRESH_TOKEN, refreshToken, expiry, secure);
      addCookie(response, FINGERPRINT, fingerprint, expiry, secure);

      // Set csrf_token cookie for double submit cookie verification (not HttpOnly so
      // client JS can read it)
      String csrfToken = calculateCsrfToken(sessionId, csrfNonce, csrfSecret);
      addCookie(response, CSRF_TOKEN, csrfToken, expiry, secure);
   }

   /**
    * Clears all auth cookies.
    */
   public static void clearAuthCookies(HttpServletResponse response, boolean secure) {
      deleteCookie(response, ACCESS_TOKEN, secure);
      deleteCookie(response, SESSION_ID, secure);
      deleteCookie(response, REFRESH_TOKEN, secure);
      deleteCookie(response, FINGERPRINT, secure);
      deleteCookie(response, CSRF_TOKEN, secure);
   }

   public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean secure) {
      boolean httpOnly = !name.equals(CSRF_TOKEN);
      String sameSite = "Lax";
      if (name.equals(REFRESH_TOKEN) || name.equals(FINGERPRINT)) {
         sameSite = "Strict";
      }
      ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(secure)
            .path("/")
            .maxAge(maxAge)
            .sameSite(sameSite);
      String resolvedDomain = resolveCookieDomain();
      if (resolvedDomain != null && !resolvedDomain.isBlank()) {
         builder.domain(resolvedDomain);
      }
      response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
   }

   public static void deleteCookie(HttpServletResponse response, String name, boolean secure) {
      boolean httpOnly = !name.equals(CSRF_TOKEN);
      String sameSite = "Lax";
      if (name.equals(REFRESH_TOKEN) || name.equals(FINGERPRINT)) {
         sameSite = "Strict";
      }
      ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
            .httpOnly(httpOnly)
            .secure(secure)
            .path("/")
            .maxAge(0)
            .sameSite(sameSite);
      String resolvedDomain = resolveCookieDomain();
      if (resolvedDomain != null && !resolvedDomain.isBlank()) {
         builder.domain(resolvedDomain);
      }
      response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
   }

   public static String getCookieValue(HttpServletRequest request, String name) {
      if (request.getCookies() != null) {
         for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
               return cookie.getValue();
            }
         }
      }

      // Manual parsing if getCookies() is null (common in server-to-server fetches)
      String cookieHeader = request.getHeader("Cookie");
      if (cookieHeader != null) {
         int nameLen = name.length();
         int headerLen = cookieHeader.length();
         int pos = 0;
         while (pos < headerLen) {
            // Trim leading spaces and semicolons
            while (pos < headerLen && (cookieHeader.charAt(pos) == ' ' || cookieHeader.charAt(pos) == ';')) {
               pos++;
            }
            if (pos >= headerLen) {
               break;
            }
            // Check if match found
            if (pos + nameLen < headerLen && cookieHeader.charAt(pos + nameLen) == '=') {
               boolean match = true;
               for (int i = 0; i < nameLen; i++) {
                  if (cookieHeader.charAt(pos + i) != name.charAt(i)) {
                     match = false;
                     break;
                  }
               }
               if (match) {
                  int valStart = pos + nameLen + 1;
                  int valEnd = valStart;
                  while (valEnd < headerLen && cookieHeader.charAt(valEnd) != ';') {
                     valEnd++;
                  }
                  // Strip quotes if value is double-quoted (RFC 6265 compliant)
                  if (valEnd - valStart > 1 && cookieHeader.charAt(valStart) == '"'
                        && cookieHeader.charAt(valEnd - 1) == '"') {
                     valStart++;
                     valEnd--;
                  }
                  return cookieHeader.substring(valStart, valEnd);
               }
            }
            // Skip current cookie pair to next semicolon
            while (pos < headerLen && cookieHeader.charAt(pos) != ';') {
               pos++;
            }
         }
      }

      return null;
   }

}
