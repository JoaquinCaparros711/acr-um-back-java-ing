package com.odontogestion.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odontogestion.api.dto.ClinicalRecordResponseDTO;
import com.odontogestion.api.dto.CompleteAppointmentRequestDTO;
import com.odontogestion.api.exception.AppointmentNotFoundException;
import com.odontogestion.api.exception.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClinicalRecordService clinicalRecordService;

    @InjectMocks
    private ClinicalRecordController clinicalRecordController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clinicalRecordController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/v1/patients/10/clinical-history - Retorna 200 OK con el historial clínico")
    void getClinicalHistory_Returns200WithList() throws Exception {
        ClinicalRecordResponseDTO record = ClinicalRecordResponseDTO.builder()
                .id(1L)
                .patientId(10L)
                .patientFirstName("Carlos")
                .patientLastName("Gómez")
                .appointmentId(100L)
                .dentistId(1L)
                .notes("Control de ortodoncia sin novedades")
                .createdAt(LocalDateTime.of(2026, 8, 10, 10, 0))
                .build();

        when(clinicalRecordService.getPatientClinicalHistory(10L)).thenReturn(List.of(record));

        mockMvc.perform(get("/api/v1/patients/10/clinical-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].patientFirstName").value("Carlos"))
                .andExpect(jsonPath("$[0].notes").value("Control de ortodoncia sin novedades"))
                .andExpect(jsonPath("$[0].dentistId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/patients/10/clinical-history - Retorna 200 OK con lista vacía")
    void getClinicalHistory_EmptyList_Returns200() throws Exception {
        when(clinicalRecordService.getPatientClinicalHistory(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/patients/10/clinical-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/patients/20/clinical-history - Paciente de otro dentista retorna 403 Forbidden")
    void getClinicalHistory_OtherDentistPatient_Returns403() throws Exception {
        when(clinicalRecordService.getPatientClinicalHistory(20L))
                .thenThrow(new SecurityException("No tiene permisos para ver el historial de este paciente"));

        mockMvc.perform(get("/api/v1/patients/20/clinical-history"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tiene permisos para ver el historial de este paciente"));
    }

    @Test
    @DisplayName("POST /api/v1/patients/10/clinical-records - Retorna 201 Created con el registro creado")
    void createClinicalRecord_Returns201Created() throws Exception {
        com.odontogestion.api.dto.CreateClinicalRecordRequestDTO request = com.odontogestion.api.dto.CreateClinicalRecordRequestDTO.builder()
                .notes("Paciente presenta leve sensibilidad en pieza 18")
                .build();

        ClinicalRecordResponseDTO responseDTO = ClinicalRecordResponseDTO.builder()
                .id(101L)
                .patientId(10L)
                .patientFirstName("Carlos")
                .patientLastName("Gómez")
                .dentistId(1L)
                .notes("Paciente presenta leve sensibilidad en pieza 18")
                .createdAt(LocalDateTime.of(2026, 8, 10, 15, 30))
                .build();

        when(clinicalRecordService.createClinicalRecord(eq(10L), any())).thenReturn(responseDTO);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/patients/10/clinical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(jsonPath("$.notes").value("Paciente presenta leve sensibilidad en pieza 18"));
    }

    @Test
    @DisplayName("POST /api/v1/patients/10/clinical-records - Notas en blanco retorna 400 Bad Request")
    void createClinicalRecord_BlankNotes_Returns400() throws Exception {
        com.odontogestion.api.dto.CreateClinicalRecordRequestDTO request = com.odontogestion.api.dto.CreateClinicalRecordRequestDTO.builder()
                .notes("   ")
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/patients/10/clinical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

