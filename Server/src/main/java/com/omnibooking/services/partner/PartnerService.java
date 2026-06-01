package com.omnibooking.services.partner;

import com.omnibooking.dto.PartnerBookingResponse;
import com.omnibooking.dto.PartnerStatsResponse;
import java.util.List;
import java.util.UUID;

public interface PartnerService {

   PartnerStatsResponse getPartnerStats(UUID partnerId);

   List<PartnerBookingResponse> getPartnerBookings(UUID partnerId);

}
