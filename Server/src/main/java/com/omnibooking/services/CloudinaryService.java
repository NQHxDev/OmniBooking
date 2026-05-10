package com.omnibooking.services;

import com.omnibooking.dto.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

public interface CloudinaryService {

   /**
    * Uploads a file to Cloudinary.
    *
    * @param file   The multipart file from request
    * @param folder The target folder in Cloudinary
    * @return Typed response containing upload details
    */
   CloudinaryResponse upload(MultipartFile file, String folder) throws IOException;

   /**
    * Uploads raw bytes to Cloudinary.
    */
   CloudinaryResponse upload(byte[] fileBytes, String folder) throws IOException;

   /**
    * Deletes a file from Cloudinary.
    *
    * @param publicId The public_id of the file to delete
    * @return Result of the deletion as a raw Map
    */
   Map<String, Object> delete(String publicId) throws IOException;

}
