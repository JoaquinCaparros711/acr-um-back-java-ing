package com.odontogestion.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odontogestion.api.dto.AppointmentRequestDTO;
import com.odontogestion.api.dto.AppointmentResponseDTO;
import com.odontogestion.api.dto.ClinicalRecordResponseDTO;
import com.odontogestion.api.dto.CompleteAppointmentRequestDTO;
import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.exception.AppointmentConflictException;
import com.odontogestion.api.exception.GlobalExceptionHandler;
import com.odontogestion.api.service.AppointmentService;
import com.odontogestion.api.service.ClinicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private ClinicalRecordService clinicalRecordService;

    @InjectMocks
    private AppointmentController appointmentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/appointments - Debe retornar HTTP 201 Created al agendar exitosamente")
    void createAppointment_Returns201Created() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(2026, 9, 1, 14, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 9, 1, 15, 0);

        AppointmentRequestDTO request = AppointmentRequestDTO.builder()
                .patientId(10L)
                .startTime(startTime)
                .endTime(endTime)
                .reason("Extracción")
                .build();

        AppointmentResponseDTO response = AppointmentResponseDTO.builder()
                .id(1L)
                .patientId(10L)
                .patientFirstName("Maria")
                .patientLastName("Lopez")
                .startTime(startTime)
                .endTime(endTime)
                .reason("Extracción")
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientFirstName").value("Maria"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("POST /api/v1/appointments - Debe retornar HTTP 409 Conflict si hay solapamiento de horario")
    void createAppointment_OverlappingConflict_Returns409() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(2026, 9, 1, 14, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 9, 1, 15, 0);

        AppointmentRequestDTO request = AppointmentRequestDTO.builder()
                .patientId(10L)
                .startTime(startTime)
                .endTime(endTime)
                .reason("Extracción")
                .build();

        when(appointmentService.createAppointment(any(AppointmentRequestDTO.class)))
                .thenThrow(new AppointmentConflictException("Conflicto de horario: Ya existe una cita agendada en ese rango horario"));

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto de horario: Ya existe una cita agendada en ese rango horario"));
    }

    @Test
    @DisplayName("GET /api/v1/appointments - Debe retornar HTTP 200 OK con la lista de citas")
    void getAppointments_Returns200OK() throws Exception {
        AppointmentResponseDTO response = AppointmentResponseDTO.builder()
                .id(1L)
                .patientId(10L)
                .patientFirstName("Maria")
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentService.getAppointments(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].patientFirstName").value("Maria"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/1/cancel - Debe retornar HTTP 200 OK con cita cancelada")
    void cancelAppointment_Returns200OK() throws Exception {
        AppointmentResponseDTO response = AppointmentResponseDTO.builder()
                .id(1L)
                .patientId(10L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentService.cancelAppointment(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/appointments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/1/complete - Con notas retorna 200 OK con el registro clínico")
    void completeAppointment_WithNotes_Returns200WithClinicalRecord() throws Exception {
        CompleteAppointmentRequestDTO request = CompleteAppointmentRequestDTO.builder()
                .notes("Extracción sin complicaciones")
                .build();

        ClinicalRecordResponseDTO clinicalRecord = ClinicalRecordResponseDTO.builder()
                .id(1L)
                .patientId(10L)
                .appointmentId(1L)
                .dentistId(1L)
                .notes("Extracción sin complicaciones")
                .createdAt(LocalDateTime.of(2026, 8, 10, 10, 0))
                .build();

        when(clinicalRecordService.completeAppointmentWithNotes(eq(1L), any(CompleteAppointmentRequestDTO.class)))
                .thenReturn(clinicalRecord);

        mockMvc.perform(patch("/api/v1/appointments/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.notes").value("Extracción sin complicaciones"))
                .andExpect(jsonPath("$.appointmentId").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/1/complete - Sin notas retorna 204 No Content")
    void completeAppointment_WithoutNotes_Returns204NoContent() throws Exception {
        when(clinicalRecordService.completeAppointmentWithNotes(eq(1L), any()))
                .thenReturn(null);

        mockMvc.perform(patch("/api/v1/appointments/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/1/complete - Cita cancelada retorna 400 Bad Request")
    void completeAppointment_CancelledAppointment_Returns400() throws Exception {
        when(clinicalRecordService.completeAppointmentWithNotes(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("No se puede completar una cita que ya fue cancelada"));

        mockMvc.perform(patch("/api/v1/appointments/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede completar una cita que ya fue cancelada"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/1/complete - Cita de otro dentista retorna 403 Forbidden")
    void completeAppointment_OtherDentist_Returns403() throws Exception {
        when(clinicalRecordService.completeAppointmentWithNotes(eq(1L), any()))
                .thenThrow(new SecurityException("No tiene permisos para completar esta cita"));

        mockMvc.perform(patch("/api/v1/appointments/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tiene permisos para completar esta cita"));
    }
}
