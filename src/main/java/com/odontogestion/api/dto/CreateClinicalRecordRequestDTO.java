package com.odontogestion.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClinicalRecordRequestDTO {

    @NotBlank(message = "Las notas clínicas no pueden estar vacías")
    private String notes;

    private Long appointmentId;
}
