package com.odontogestion.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odontogestion.api.dto.FinancialSummaryResponseDTO;
import com.odontogestion.api.entity.PaymentMethod;
import com.odontogestion.api.exception.GlobalExceptionHandler;
import com.odontogestion.api.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinancialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private FinancialController financialController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(financialController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/finance/summary - Retorna 200 OK con el resumen financiero")
    void getFinancialSummary_Returns200OK() throws Exception {
        FinancialSummaryResponseDTO summaryDTO = FinancialSummaryResponseDTO.builder()
                .totalIncome(new BigDecimal("1500.00"))
                .totalPending(new BigDecimal("500.00"))
                .paidCount(3L)
                .pendingCount(1L)
                .incomeByMethod(Map.of(PaymentMethod.CASH, new BigDecimal("1500.00")))
                .build();

        when(appointmentService.getFinancialSummary(any(), any())).thenReturn(summaryDTO);

        mockMvc.perform(get("/api/v1/finance/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1500.00))
                .andExpect(jsonPath("$.totalPending").value(500.00))
                .andExpect(jsonPath("$.paidCount").value(3))
                .andExpect(jsonPath("$.pendingCount").value(1));
    }
}
