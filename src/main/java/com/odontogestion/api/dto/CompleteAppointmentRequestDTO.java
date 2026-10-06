package com.odontogestion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteAppointmentRequestDTO {

    /**
     * Notas clínicas opcionales a registrar al completar la cita.
     */
    private String notes;
}
