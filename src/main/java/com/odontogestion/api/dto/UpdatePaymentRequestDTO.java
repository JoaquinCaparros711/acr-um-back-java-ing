package com.odontogestion.api.dto;

import com.odontogestion.api.entity.PaymentMethod;
import com.odontogestion.api.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentRequestDTO {

    @NotNull(message = "El estado de pago es obligatorio")
    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    private LocalDateTime paymentDate;

    @DecimalMin(value = "0.0", message = "El monto no puede ser negativo")
    private BigDecimal amount;

    private String paymentNotes;
}
