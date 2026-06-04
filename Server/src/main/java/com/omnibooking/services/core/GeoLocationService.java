package com.omnibooking.services.core;

public interface GeoLocationService {

   /**
    * Trả về mã quốc gia (ISO 3166-1 alpha-2) dựa trên địa chỉ IP.
    *
    * @param ipAddress Địa chỉ IP của người dùng.
    * @return Mã quốc gia (ví dụ: "VN", "US"). Trả về mã mặc định nếu không xác
    *         định được.
    */
   String getCountryCode(String ipAddress);

}
