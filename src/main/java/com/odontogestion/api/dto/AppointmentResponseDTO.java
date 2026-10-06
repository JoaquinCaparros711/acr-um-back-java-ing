package com.odontogestion.api.dto;

import com.odontogestion.api.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {

    private Long id;
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private String patientDni;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private AppointmentStatus status;
    private com.odontogestion.api.entity.PaymentStatus paymentStatus;
    private com.odontogestion.api.entity.PaymentMethod paymentMethod;
    private LocalDateTime paymentDate;
    private java.math.BigDecimal amount;
    private String paymentNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
