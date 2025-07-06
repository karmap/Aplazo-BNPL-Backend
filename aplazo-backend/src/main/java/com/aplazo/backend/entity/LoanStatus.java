package com.aplazo.backend.entity;

public enum LoanStatus {
    ACTIVE,     // has pending installment payments
    LATE,       // has installment payments with error
    COMPLETED   // all installments are paid
} 