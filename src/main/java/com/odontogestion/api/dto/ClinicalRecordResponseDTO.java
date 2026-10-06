package com.odontogestion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalRecordResponseDTO {

    private Long id;
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private Long appointmentId;
    private Long dentistId;
    private String notes;
    private LocalDateTime createdAt;
}
