package com.odontogestion.api.repository;

import com.odontogestion.api.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByUserId(Long userId);

    Optional<Patient> findByDniAndUserId(String dni, Long userId);

    @Query("SELECT p FROM Patient p WHERE p.user.id = :userId AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.dni LIKE CONCAT('%', :query, '%'))")
    List<Patient> searchPatients(@Param("userId") Long userId, @Param("query") String query);
}
