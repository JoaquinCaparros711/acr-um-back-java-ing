package com.odontogestion.api.controller;

import com.odontogestion.api.dto.ClinicalRecordResponseDTO;
import com.odontogestion.api.service.ClinicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class ClinicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    /**
     * Retorna el historial clínico de un paciente perteneciente al odontólogo autenticado,
     * ordenado del registro más reciente al más antiguo.
     *
     * @param patientId ID del paciente
     * @return 200 OK con la lista de registros clínicos
     */
    @GetMapping("/{patientId}/clinical-history")
    public ResponseEntity<List<ClinicalRecordResponseDTO>> getClinicalHistory(
            @PathVariable Long patientId
    ) {
        return ResponseEntity.ok(clinicalRecordService.getPatientClinicalHistory(patientId));
    }

    /**
     * Registra una nueva evolución clínica para el paciente especificado.
     *
     * @param patientId ID del paciente
     * @param request DTO con la evolución clínica a registrar
     * @return 201 Created con el registro clínico creado
     */
    @org.springframework.web.bind.annotation.PostMapping("/{patientId}/clinical-records")
    public ResponseEntity<ClinicalRecordResponseDTO> createClinicalRecord(
            @PathVariable Long patientId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.odontogestion.api.dto.CreateClinicalRecordRequestDTO request
    ) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(clinicalRecordService.createClinicalRecord(patientId, request));
    }
}
