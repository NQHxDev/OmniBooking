package com.omnibooking.repository.registration;

import com.omnibooking.model.RegistrationDlt;
import com.omnibooking.model.enums.RegistrationDltStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegistrationDltRepository extends JpaRepository<RegistrationDlt, UUID> {

   List<RegistrationDlt> findByStatus(RegistrationDltStatus status);

   List<RegistrationDlt> findByPartitionIdAndStatus(Integer partitionId, RegistrationDltStatus status);

   long countByStatus(RegistrationDltStatus status);

}
