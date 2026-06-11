package com.omnibooking.constant;

public final class MediaConstants {

   private MediaConstants() {
      // Private constructor to prevent instantiation
   }

   public static final String PROPERTY_TYPE = "PROPERTY";
   public static final String ROOM_TYPE = "ROOM_TYPE";
   public static final String USER_AVATAR_TYPE = "USER_AVATAR";

   public static final String COMMON_FOLDER = "common";

   public static String getPropertyFolder(String propertiesBaseFolder, String propertyId) {
      return propertiesBaseFolder + "/" + propertyId;
   }

   public static String getPropertyPublicId(String propertiesBaseFolder, String propertyId, String fileId) {
      return getPropertyFolder(propertiesBaseFolder, propertyId) + "/" + fileId;
   }

   public static String getCloudinaryUrl(String cloudName, String publicId, String format) {
      return "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId + "." + format;
   }

}
