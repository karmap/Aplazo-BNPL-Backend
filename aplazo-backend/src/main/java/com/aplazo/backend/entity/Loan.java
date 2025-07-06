package com.aplazo.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_scheme", nullable = false)
    private String paymentScheme;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Installment> installments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = LoanStatus.ACTIVE;
        calculatePaymentDetails();
    }

    public void calculatePaymentDetails() {
        // Calculate commission based on payment scheme
        if ("SCHEME_1".equals(paymentScheme)) {
            interestRate = new BigDecimal("13.00");
        } else {
            interestRate = new BigDecimal("16.00");
        }

        // Calculate commission amount
        commissionAmount = amount.multiply(interestRate).divide(new BigDecimal("100"));
        totalAmount = amount.add(commissionAmount);
    }

    public void updateStatus() {
        if (installments == null || installments.isEmpty()) {
            status = LoanStatus.ACTIVE;
            return;
        }

        long errorInstallments = installments.stream()
                .filter(installment -> InstallmentStatus.ERROR.equals(installment.getStatus()))
                .count();

        long completedInstallments = installments.stream()
                .filter(installment -> InstallmentStatus.COMPLETED.equals(installment.getStatus()))
                .count();

        if (errorInstallments > 0) {
            status = LoanStatus.LATE;
        } else if (completedInstallments == installments.size()) {
            status = LoanStatus.COMPLETED;
        } else {
            status = LoanStatus.ACTIVE;
        }
    }
} 