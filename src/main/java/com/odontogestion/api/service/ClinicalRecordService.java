package com.odontogestion.api.service;

import com.odontogestion.api.dto.ClinicalRecordResponseDTO;
import com.odontogestion.api.dto.CompleteAppointmentRequestDTO;
import com.odontogestion.api.dto.CreateClinicalRecordRequestDTO;
import com.odontogestion.api.entity.Appointment;
import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.entity.ClinicalRecord;
import com.odontogestion.api.entity.Patient;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.AppointmentNotFoundException;
import com.odontogestion.api.exception.PatientNotFoundException;
import com.odontogestion.api.repository.AppointmentRepository;
import com.odontogestion.api.repository.ClinicalRecordRepository;
import com.odontogestion.api.repository.PatientRepository;
import com.odontogestion.api.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalRecordService {

    private final ClinicalRecordRepository clinicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final AuthenticationFacade authenticationFacade;

    /**
     * Completes an appointment and optionally persists a clinical record entry.
     *
     * @param appointmentId ID of appointment to complete
     * @param request DTO containing optional clinical notes
     * @return DTO with created clinical record response, or null if no notes were supplied
     */
    @Transactional
    public ClinicalRecordResponseDTO completeAppointmentWithNotes(
            Long appointmentId,
            CompleteAppointmentRequestDTO request
    ) {
        User dentist = authenticationFacade.getAuthenticatedUser();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (!appointment.getUser().getId().equals(dentist.getId())) {
            throw new SecurityException("Access denied for completing appointment ID: " + appointmentId);
        }

        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Cannot complete an appointment that has already been cancelled");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        boolean hasNotes = request != null
                && request.getNotes() != null
                && !request.getNotes().isBlank();

        if (!hasNotes) {
            return null;
        }

        ClinicalRecord record = clinicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> ClinicalRecord.builder()
                        .patient(appointment.getPatient())
                        .appointment(appointment)
                        .dentist(dentist)
                        .build());

        record.setNotes(request.getNotes().trim());
        ClinicalRecord saved = clinicalRecordRepository.save(record);
        return mapToResponse(saved);
    }

    /**
     * Retrieves the clinical history of a patient belonging to the authenticated dentist.
     *
     * @param patientId ID of the patient
     * @return List of clinical records ordered by creation date descending
     */
    @Transactional(readOnly = true)
    public List<ClinicalRecordResponseDTO> getPatientClinicalHistory(Long patientId) {
        User dentist = authenticationFacade.getAuthenticatedUser();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));

        if (!patient.getUser().getId().equals(dentist.getId())) {
            throw new SecurityException("Access denied for viewing clinical history of patient ID: " + patientId);
        }

        return clinicalRecordRepository
                .findByPatientIdAndDentistIdOrderByCreatedAtDesc(patientId, dentist.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Creates a direct clinical evolution record entry for a patient.
     *
     * @param patientId ID of the patient
     * @param request DTO with clinical record notes
     * @return DTO with created clinical record response
     */
    @Transactional
    public ClinicalRecordResponseDTO createClinicalRecord(Long patientId, CreateClinicalRecordRequestDTO request) {
        User dentist = authenticationFacade.getAuthenticatedUser();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));

        if (!patient.getUser().getId().equals(dentist.getId())) {
            throw new SecurityException("Access denied for adding clinical records to patient ID: " + patientId);
        }

        Appointment appointment = null;
        if (request != null && request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));
            if (!appointment.getPatient().getId().equals(patientId) || !appointment.getUser().getId().equals(dentist.getId())) {
                throw new IllegalArgumentException("Appointment does not match requested patient or dentist context");
            }
        }

        ClinicalRecord record = ClinicalRecord.builder()
                .patient(patient)
                .appointment(appointment)
                .dentist(dentist)
                .notes(request != null && request.getNotes() != null ? request.getNotes().trim() : "")
                .build();

        ClinicalRecord saved = clinicalRecordRepository.save(record);
        return mapToResponse(saved);
    }

    private ClinicalRecordResponseDTO mapToResponse(ClinicalRecord record) {
        return ClinicalRecordResponseDTO.builder()
                .id(record.getId())
                .patientId(record.getPatient().getId())
                .patientFirstName(record.getPatient().getFirstName())
                .patientLastName(record.getPatient().getLastName())
                .appointmentId(record.getAppointment() != null ? record.getAppointment().getId() : null)
                .dentistId(record.getDentist().getId())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
