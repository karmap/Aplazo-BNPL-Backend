package com.aplazo.backend.entity;

public enum InstallmentStatus {
    NEXT,       // marked as next installment to be paid
    PENDING,    // installment still pending to be paid
    ERROR,      // error on payment, considered unpaid
    COMPLETED   // installment has been paid successfully
} 