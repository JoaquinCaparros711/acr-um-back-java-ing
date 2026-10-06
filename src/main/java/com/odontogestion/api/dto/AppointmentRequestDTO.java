package com.odontogestion.api.dto;

import com.odontogestion.api.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    private LocalDateTime startTime;

    @NotNull(message = "La fecha y hora de fin es obligatoria")
    private LocalDateTime endTime;

    private String reason;

    private AppointmentStatus status;

    private java.math.BigDecimal amount;

    private com.odontogestion.api.entity.PaymentStatus paymentStatus;

    private com.odontogestion.api.entity.PaymentMethod paymentMethod;

    private String paymentNotes;
}
