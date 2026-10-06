package com.odontogestion.api.dto;

import com.odontogestion.api.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryResponseDTO {

    private BigDecimal totalIncome;
    private BigDecimal totalPending;
    private Long paidCount;
    private Long pendingCount;
    private Map<PaymentMethod, BigDecimal> incomeByMethod;
}
