package com.odontogestion.api.repository;

import com.odontogestion.api.entity.Appointment;
import com.odontogestion.api.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrderByStartTimeAsc(Long userId);

    List<Appointment> findByUserIdAndPatientIdOrderByStartTimeAsc(Long userId, Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.user.id = :userId " +
           "AND a.startTime >= :startOfDay AND a.startTime <= :endOfDay " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.user.id = :userId " +
           "AND a.status <> :cancelledStatus " +
           "AND (:excludeId IS NULL OR a.id <> :excludeId) " +
           "AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean existsOverlappingAppointment(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );

    long countByUserIdAndStartTimeBetweenAndStatusNot(
            Long userId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            AppointmentStatus status
    );
}
