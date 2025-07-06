package com.aplazo.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "scheduled_payment_date", nullable = false)
    private LocalDate scheduledPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallmentStatus status;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = InstallmentStatus.PENDING;
        }
    }
} 