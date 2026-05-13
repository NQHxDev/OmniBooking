package com.omnibooking.dto.oauth;

import lombok.Data;

@Data
public class ZaloUserInfo implements OAuth2UserInfo {

   private String id;

   private String name;

   private String email;

   private Picture picture;

   @Data
   public static class Picture {
      private PictureData data;

      @Data
      public static class PictureData {
         private String url;
      }
   }

   @Override
   public String getPicture() {
      return (picture != null && picture.getData() != null) ? picture.getData().getUrl() : null;
   }

}
