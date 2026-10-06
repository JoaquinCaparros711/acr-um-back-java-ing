package com.odontogestion.api.repository;

import com.odontogestion.api.entity.ClinicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, Long> {

    List<ClinicalRecord> findByPatientIdAndDentistIdOrderByCreatedAtDesc(Long patientId, Long dentistId);

    boolean existsByAppointmentId(Long appointmentId);

    Optional<ClinicalRecord> findByAppointmentId(Long appointmentId);
}
