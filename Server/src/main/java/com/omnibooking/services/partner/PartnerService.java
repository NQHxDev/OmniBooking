package com.omnibooking.services.partner;

import com.omnibooking.dto.PartnerStatsResponse;
import java.util.UUID;

public interface PartnerService {

   PartnerStatsResponse getPartnerStats(UUID partnerId);

}
