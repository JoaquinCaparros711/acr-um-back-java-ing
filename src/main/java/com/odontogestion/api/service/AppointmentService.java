package com.odontogestion.api.service;

import com.odontogestion.api.dto.AppointmentRequestDTO;
import com.odontogestion.api.dto.AppointmentResponseDTO;
import com.odontogestion.api.dto.FinancialSummaryResponseDTO;
import com.odontogestion.api.dto.UpdatePaymentRequestDTO;
import com.odontogestion.api.entity.Appointment;
import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.entity.Patient;
import com.odontogestion.api.entity.PaymentMethod;
import com.odontogestion.api.entity.PaymentStatus;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.AppointmentConflictException;
import com.odontogestion.api.exception.AppointmentNotFoundException;
import com.odontogestion.api.exception.InvalidAppointmentTimeException;
import com.odontogestion.api.exception.PatientNotFoundException;
import com.odontogestion.api.repository.AppointmentRepository;
import com.odontogestion.api.repository.PatientRepository;
import com.odontogestion.api.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final AuthenticationFacade authenticationFacade;

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointments(LocalDate date, Long patientId) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        List<Appointment> appointments;

        if (date != null) {
            appointments = appointmentRepository.findByUserIdAndDateRange(
                    dentist.getId(),
                    date.atStartOfDay(),
                    date.atTime(LocalTime.MAX)
            );
        } else if (patientId != null) {
            appointments = appointmentRepository.findByUserIdAndPatientIdOrderByStartTimeAsc(
                    dentist.getId(),
                    patientId
            );
        } else {
            appointments = appointmentRepository.findByUserIdOrderByStartTimeAsc(dentist.getId());
        }

        return appointments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(Long id) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Appointment appointment = findAppointmentAndValidateOwnership(id, dentist.getId());
        return mapToResponse(appointment);
    }

    @Transactional
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO request) {
        User dentist = authenticationFacade.getAuthenticatedUser();

        validateTimes(request);
        Patient patient = findPatientAndValidateOwnership(request.getPatientId(), dentist.getId());

        checkForConflicts(dentist.getId(), request.getStartTime(), request.getEndTime(), null);

        AppointmentStatus status = request.getStatus() != null ? request.getStatus() : AppointmentStatus.SCHEDULED;

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .user(dentist)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .status(status)
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .paymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentNotes(request.getPaymentNotes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO request) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Appointment appointment = findAppointmentAndValidateOwnership(id, dentist.getId());

        validateTimes(request);
        Patient patient = findPatientAndValidateOwnership(request.getPatientId(), dentist.getId());

        checkForConflicts(dentist.getId(), request.getStartTime(), request.getEndTime(), id);

        appointment.setPatient(patient);
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setReason(request.getReason());
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }
        if (request.getAmount() != null) {
            appointment.setAmount(request.getAmount());
        }
        if (request.getPaymentStatus() != null) {
            appointment.setPaymentStatus(request.getPaymentStatus());
        }
        if (request.getPaymentMethod() != null) {
            appointment.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getPaymentNotes() != null) {
            appointment.setPaymentNotes(request.getPaymentNotes());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    @Transactional
    public AppointmentResponseDTO updatePayment(Long id, UpdatePaymentRequestDTO request) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Appointment appointment = findAppointmentAndValidateOwnership(id, dentist.getId());

        if (request.getPaymentStatus() == PaymentStatus.PAID && request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required when marking appointment as PAID");
        }

        appointment.setPaymentStatus(request.getPaymentStatus());
        appointment.setPaymentMethod(request.getPaymentMethod());

        if (request.getAmount() != null) {
            appointment.setAmount(request.getAmount());
        }

        if (request.getPaymentNotes() != null) {
            appointment.setPaymentNotes(request.getPaymentNotes());
        }

        if (request.getPaymentStatus() == PaymentStatus.PAID) {
            appointment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now());
        } else {
            appointment.setPaymentDate(null);
            appointment.setPaymentMethod(null);
        }

        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponseDTO getFinancialSummary(LocalDate startDate, LocalDate endDate) {
        User dentist = authenticationFacade.getAuthenticatedUser();

        List<Appointment> appointments;
        if (startDate != null && endDate != null) {
            appointments = appointmentRepository.findByUserIdAndDateRange(
                    dentist.getId(),
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX)
            );
        } else {
            appointments = appointmentRepository.findByUserIdOrderByStartTimeAsc(dentist.getId());
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        long paidCount = 0;
        long pendingCount = 0;

        Map<PaymentMethod, BigDecimal> incomeByMethod = new HashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            incomeByMethod.put(method, BigDecimal.ZERO);
        }

        for (Appointment app : appointments) {
            BigDecimal amount = app.getAmount() != null ? app.getAmount() : BigDecimal.ZERO;

            if (app.getPaymentStatus() == PaymentStatus.PAID) {
                totalIncome = totalIncome.add(amount);
                paidCount++;
                if (app.getPaymentMethod() != null) {
                    incomeByMethod.put(app.getPaymentMethod(), incomeByMethod.get(app.getPaymentMethod()).add(amount));
                }
            } else {
                totalPending = totalPending.add(amount);
                pendingCount++;
            }
        }

        return FinancialSummaryResponseDTO.builder()
                .totalIncome(totalIncome)
                .totalPending(totalPending)
                .paidCount(paidCount)
                .pendingCount(pendingCount)
                .incomeByMethod(incomeByMethod)
                .build();
    }

    @Transactional
    public AppointmentResponseDTO cancelAppointment(Long id) {
        User dentist = authenticationFacade.getAuthenticatedUser();
        Appointment appointment = findAppointmentAndValidateOwnership(id, dentist.getId());

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    private Appointment findAppointmentAndValidateOwnership(Long id, Long userId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + id));

        if (!appointment.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied for requested appointment ID: " + id);
        }
        return appointment;
    }

    private Patient findPatientAndValidateOwnership(Long patientId, Long userId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));

        if (!patient.getUser().getId().equals(userId)) {
            throw new SecurityException("Patient does not belong to authenticated dentist");
        }
        return patient;
    }

    private void validateTimes(AppointmentRequestDTO request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new InvalidAppointmentTimeException("Start time and end time are required");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new InvalidAppointmentTimeException("Start time must be strictly before end time");
        }
    }

    private void checkForConflicts(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        boolean hasConflict = appointmentRepository.existsOverlappingAppointment(
                userId,
                startTime,
                endTime,
                excludeId,
                AppointmentStatus.CANCELLED
        );
        if (hasConflict) {
            throw new AppointmentConflictException("Schedule conflict detected: An appointment already exists in this time range");
        }
    }

    private AppointmentResponseDTO mapToResponse(Appointment appointment) {
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .patientFirstName(appointment.getPatient().getFirstName())
                .patientLastName(appointment.getPatient().getLastName())
                .patientDni(appointment.getPatient().getDni())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .reason(appointment.getReason())
                .status(appointment.getStatus())
                .paymentStatus(appointment.getPaymentStatus() != null ? appointment.getPaymentStatus() : PaymentStatus.PENDING)
                .paymentMethod(appointment.getPaymentMethod())
                .paymentDate(appointment.getPaymentDate())
                .amount(appointment.getAmount() != null ? appointment.getAmount() : BigDecimal.ZERO)
                .paymentNotes(appointment.getPaymentNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
