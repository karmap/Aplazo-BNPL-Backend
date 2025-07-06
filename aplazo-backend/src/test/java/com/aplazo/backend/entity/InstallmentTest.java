package com.aplazo.backend.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

class InstallmentTest {

    @Test
    void testInstallmentCreation() {
        // Given
        LocalDate scheduledDate = LocalDate.now();
        BigDecimal amount = new BigDecimal("100.00");
        InstallmentStatus status = InstallmentStatus.PENDING;

        // When
        Installment installment = new Installment();
        installment.setScheduledPaymentDate(scheduledDate);
        installment.setAmount(amount);
        installment.setStatus(status);

        // Then
        assertEquals(scheduledDate, installment.getScheduledPaymentDate());
        assertEquals(amount, installment.getAmount());
        assertEquals(status, installment.getStatus());
    }

    @Test
    void testInstallmentWithLoan() {
        // Given
        Loan loan = new Loan();
        loan.setId(java.util.UUID.randomUUID());
        
        Installment installment = new Installment();
        installment.setLoan(loan);

        // Then
        assertEquals(loan, installment.getLoan());
    }
} 