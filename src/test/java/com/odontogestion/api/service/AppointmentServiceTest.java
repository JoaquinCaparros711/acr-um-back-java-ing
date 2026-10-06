package com.odontogestion.api.service;

import com.odontogestion.api.dto.AppointmentRequestDTO;
import com.odontogestion.api.dto.AppointmentResponseDTO;
import com.odontogestion.api.entity.Appointment;
import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.entity.Patient;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.AppointmentConflictException;
import com.odontogestion.api.exception.AppointmentNotFoundException;
import com.odontogestion.api.exception.InvalidAppointmentTimeException;
import com.odontogestion.api.exception.PatientNotFoundException;
import com.odontogestion.api.repository.AppointmentRepository;
import com.odontogestion.api.repository.PatientRepository;
import com.odontogestion.api.security.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private AppointmentService appointmentService;

    private User mockUser;
    private Patient mockPatient;
    private LocalDateTime baseStartTime;
    private LocalDateTime baseEndTime;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
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
                .user(mockUser)
                .build();

        baseStartTime = LocalDateTime.of(2026, 8, 10, 10, 0);
        baseEndTime = LocalDateTime.of(2026, 8, 10, 11, 0);
    }

    @Test
    @DisplayName("Should create appointment successfully when no schedule conflict exists")
    void createAppointment_ShouldSucceed_WhenNoConflictExists() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        AppointmentRequestDTO request = AppointmentRequestDTO.builder()
                .patientId(10L)
                .startTime(baseStartTime)
                .endTime(baseEndTime)
                .reason("Dental Cleaning")
                .build();

        when(patientRepository.findById(10L)).thenReturn(Optional.of(mockPatient));
        when(appointmentRepository.existsOverlappingAppointment(
                eq(1L), eq(baseStartTime), eq(baseEndTime), eq(null), eq(AppointmentStatus.CANCELLED)
        )).thenReturn(false);

        Appointment savedAppointment = Appointment.builder()
                .id(100L)
                .patient(mockPatient)
                .user(mockUser)
                .startTime(baseStartTime)
                .endTime(baseEndTime)
                .reason("Dental Cleaning")
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        // Act
        AppointmentResponseDTO response = appointmentService.createAppointment(request);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Dental Cleaning", response.getReason());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentTimeException when startTime is after endTime")
    void createAppointment_ShouldThrowException_WhenInvalidTimeRange() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        AppointmentRequestDTO request = AppointmentRequestDTO.builder()
                .patientId(10L)
                .startTime(baseEndTime)
                .endTime(baseStartTime)
                .build();

        // Act & Assert
        assertThrows(InvalidAppointmentTimeException.class,
                () -> appointmentService.createAppointment(request));

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PatientNotFoundException when patient does not exist")
    void createAppointment_ShouldThrowException_WhenPatientNotFound() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        AppointmentRequestDTO request = AppointmentRequestDTO.builder()
                .patientId(999L)
                .startTime(baseStartTime)
                .endTime(baseEndTime)
                .build();

        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PatientNotFoundException.class,
                () -> appointmentService.createAppointment(request));
    }

    @Test
    @DisplayName("Should throw AppointmentConflictException when schedule overlaps")
    void createAppointment_ShouldThrowException_WhenScheduleOverlaps() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        AppointmentRequestDTO request = AppointmentRequestDTO.builder()
                .patientId(10L)
                .startTime(baseStartTime)
                .endTime(baseEndTime)
                .build();

        when(patientRepository.findById(10L)).thenReturn(Optional.of(mockPatient));
        when(appointmentRepository.existsOverlappingAppointment(
                eq(1L), eq(baseStartTime), eq(baseEndTime), eq(null), eq(AppointmentStatus.CANCELLED)
        )).thenReturn(true);

        // Act & Assert
        assertThrows(AppointmentConflictException.class,
                () -> appointmentService.createAppointment(request));
    }

    @Test
    @DisplayName("Should retrieve appointment by ID successfully for owner dentist")
    void getAppointmentById_ShouldReturnAppointment_WhenOwnerMatches() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        Appointment appointment = Appointment.builder()
                .id(100L)
                .patient(mockPatient)
                .user(mockUser)
                .startTime(baseStartTime)
                .endTime(baseEndTime)
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));

        // Act
        AppointmentResponseDTO response = appointmentService.getAppointmentById(100L);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Should throw AppointmentNotFoundException when appointment ID does not exist")
    void getAppointmentById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppointmentNotFoundException.class,
                () -> appointmentService.getAppointmentById(999L));
    }
}
