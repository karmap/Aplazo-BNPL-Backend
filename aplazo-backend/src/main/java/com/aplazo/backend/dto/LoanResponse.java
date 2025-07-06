package com.aplazo.backend.dto;

import com.aplazo.backend.entity.LoanStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class LoanResponse {

    private UUID id;

    private UUID customerId;

    private BigDecimal amount;

    private LoanStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private PaymentPlan paymentPlan;

    @Data
    public static class PaymentPlan {
        private BigDecimal commissionAmount;
        private List<InstallmentResponse> installments;
    }

    @Data
    public static class InstallmentResponse {
        private BigDecimal amount;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private String scheduledPaymentDate;

        private String status;
    }
} 