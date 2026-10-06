package com.odontogestion.api.service;

import com.odontogestion.api.dto.ClinicalRecordResponseDTO;
import com.odontogestion.api.dto.CompleteAppointmentRequestDTO;
import com.odontogestion.api.entity.Appointment;
import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.entity.ClinicalRecord;
import com.odontogestion.api.entity.Patient;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.AppointmentNotFoundException;
import com.odontogestion.api.exception.PatientNotFoundException;
import com.odontogestion.api.repository.AppointmentRepository;
import com.odontogestion.api.repository.ClinicalRecordRepository;
import com.odontogestion.api.repository.PatientRepository;
import com.odontogestion.api.security.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordServiceTest {

    @Mock
    private ClinicalRecordRepository clinicalRecordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private ClinicalRecordService clinicalRecordService;

    private User mockDentist;
    private Patient mockPatient;
    private Appointment mockAppointment;

    @BeforeEach
    void setUp() {
        mockDentist = User.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Pérez")
                .email("dentist@odontogestion.com")
                .password("encoded_password")
                .role(Role.DENTIST)
                .build();

        mockPatient = Patient.builder()
                .id(10L)
                .firstName("Carlos")
                .lastName("Gómez")
                .dni("12345678")
                .birthDate(LocalDate.of(1990, 5, 20))
                .user(mockDentist)
                .build();

        mockAppointment = Appointment.builder()
                .id(100L)
                .patient(mockPatient)
                .user(mockDentist)
                .startTime(LocalDateTime.of(2026, 8, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 8, 10, 11, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("Should complete appointment and persist clinical record when notes are supplied")
    void completeAppointmentWithNotes_ShouldCreateRecord_WhenNotesProvided() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockDentist);
        CompleteAppointmentRequestDTO request = new CompleteAppointmentRequestDTO("Patient presented minor cavity");

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(mockAppointment));
        when(clinicalRecordRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());

        ClinicalRecord savedRecord = ClinicalRecord.builder()
                .id(50L)
                .patient(mockPatient)
                .appointment(mockAppointment)
                .dentist(mockDentist)
                .notes("Patient presented minor cavity")
                .createdAt(LocalDateTime.now())
                .build();

        when(clinicalRecordRepository.save(any(ClinicalRecord.class))).thenReturn(savedRecord);

        // Act
        ClinicalRecordResponseDTO result = clinicalRecordService.completeAppointmentWithNotes(100L, request);

        // Assert
        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals("Patient presented minor cavity", result.getNotes());
        assertEquals(AppointmentStatus.COMPLETED, mockAppointment.getStatus());
        verify(appointmentRepository).save(mockAppointment);
    }

    @Test
    @DisplayName("Should complete appointment without creating clinical record when notes are empty")
    void completeAppointmentWithNotes_ShouldNotCreateRecord_WhenNotesEmpty() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockDentist);
        CompleteAppointmentRequestDTO request = new CompleteAppointmentRequestDTO("   ");

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(mockAppointment));

        // Act
        ClinicalRecordResponseDTO result = clinicalRecordService.completeAppointmentWithNotes(100L, request);

        // Assert
        assertNull(result);
        assertEquals(AppointmentStatus.COMPLETED, mockAppointment.getStatus());
        verify(appointmentRepository).save(mockAppointment);
        verify(clinicalRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when attempting to complete a cancelled appointment")
    void completeAppointmentWithNotes_ShouldThrowException_WhenAppointmentIsCancelled() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockDentist);
        mockAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(mockAppointment));

        CompleteAppointmentRequestDTO request = new CompleteAppointmentRequestDTO("Notes");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> clinicalRecordService.completeAppointmentWithNotes(100L, request));
    }

    @Test
    @DisplayName("Should retrieve patient clinical history in descending order for owner dentist")
    void getPatientClinicalHistory_ShouldReturnHistory_WhenAuthorized() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockDentist);
        when(patientRepository.findById(10L)).thenReturn(Optional.of(mockPatient));

        ClinicalRecord record = ClinicalRecord.builder()
                .id(1L)
                .patient(mockPatient)
                .dentist(mockDentist)
                .notes("History note 1")
                .createdAt(LocalDateTime.now())
                .build();

        when(clinicalRecordRepository.findByPatientIdAndDentistIdOrderByCreatedAtDesc(10L, 1L))
                .thenReturn(List.of(record));

        // Act
        List<ClinicalRecordResponseDTO> history = clinicalRecordService.getPatientClinicalHistory(10L);

        // Assert
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("History note 1", history.get(0).getNotes());
    }

    @Test
    @DisplayName("Should throw PatientNotFoundException when patient ID does not exist")
    void getPatientClinicalHistory_ShouldThrowException_WhenPatientNotFound() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockDentist);
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PatientNotFoundException.class,
                () -> clinicalRecordService.getPatientClinicalHistory(999L));
    }
}
