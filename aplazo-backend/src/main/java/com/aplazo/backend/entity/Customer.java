package com.aplazo.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "second_last_name", nullable = false)
    private String secondLastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "credit_line_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditLineAmount;

    @Column(name = "available_credit_line_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal availableCreditLineAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculateCreditLine();
        availableCreditLineAmount = creditLineAmount;
    }

    public void calculateCreditLine() {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        
        if (age < 18 || age > 65) {
            throw new IllegalArgumentException("Customer age must be between 18 and 65 years");
        }

        if (age >= 18 && age <= 25) {
            creditLineAmount = new BigDecimal("3000.00");
        } else if (age >= 26 && age <= 30) {
            creditLineAmount = new BigDecimal("5000.00");
        } else if (age >= 31 && age <= 65) {
            creditLineAmount = new BigDecimal("8000.00");
        }
    }

    public void updateAvailableCreditLine(BigDecimal usedAmount) {
        this.availableCreditLineAmount = this.creditLineAmount.subtract(usedAmount);
        if (this.availableCreditLineAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient credit line available");
        }
    }

    public boolean hasSufficientCredit(BigDecimal amount) {
        return availableCreditLineAmount.compareTo(amount) >= 0;
    }

    public String getPaymentScheme() {
        // Rule 1: If first name starts with C, L, or H, assign Scheme 1
        if (firstName != null && !firstName.isEmpty()) {
            char firstChar = Character.toUpperCase(firstName.charAt(0));
            if (firstChar == 'C' || firstChar == 'L' || firstChar == 'H') {
                return "SCHEME_1";
            }
        }
        
        // Rule 2: If customer ID is greater than 25, assign Scheme 2
        if (id != null && id.toString().hashCode() > 25) {
            return "SCHEME_2";
        }
        
        // Default: Scheme 2
        return "SCHEME_2";
    }
} 