package com.odontogestion.api.controller;

import com.odontogestion.api.dto.AppointmentRequestDTO;
import com.odontogestion.api.dto.AppointmentResponseDTO;
import com.odontogestion.api.dto.ClinicalRecordResponseDTO;
import com.odontogestion.api.dto.CompleteAppointmentRequestDTO;
import com.odontogestion.api.service.AppointmentService;
import com.odontogestion.api.service.ClinicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ClinicalRecordService clinicalRecordService;

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long patientId
    ) {
        return ResponseEntity.ok(appointmentService.getAppointments(date, patientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequestDTO request
    ) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ClinicalRecordResponseDTO> completeAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteAppointmentRequestDTO request
    ) {
        ClinicalRecordResponseDTO result = clinicalRecordService.completeAppointmentWithNotes(id, request);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<AppointmentResponseDTO> updatePayment(
            @PathVariable Long id,
            @Valid @RequestBody com.odontogestion.api.dto.UpdatePaymentRequestDTO request
    ) {
        return ResponseEntity.ok(appointmentService.updatePayment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> deleteAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
