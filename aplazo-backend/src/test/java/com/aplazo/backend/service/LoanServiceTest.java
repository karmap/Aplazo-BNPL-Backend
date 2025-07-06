package com.aplazo.backend.service;

import com.aplazo.backend.dto.LoanRequest;
import com.aplazo.backend.dto.LoanResponse;
import com.aplazo.backend.entity.Customer;
import com.aplazo.backend.entity.Installment;
import com.aplazo.backend.entity.Loan;
import com.aplazo.backend.entity.LoanStatus;
import com.aplazo.backend.repository.CustomerRepository;
import com.aplazo.backend.repository.InstallmentRepository;
import com.aplazo.backend.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private LoanService loanService;

    private LoanRequest validLoanRequest;
    private Customer validCustomer;
    private Loan validLoan;
    private UUID customerId;
    private UUID loanId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        loanId = UUID.randomUUID();

        validLoanRequest = new LoanRequest();
        validLoanRequest.setCustomerId(customerId);
        validLoanRequest.setAmount(new BigDecimal("1000.00"));

        validCustomer = Customer.builder()
                .id(customerId)
                .firstName("Juan")
                .lastName("López")
                .secondLastName("Pérez")
                .dateOfBirth(LocalDate.of(1998, 7, 21))
                .creditLineAmount(new BigDecimal("5000.00"))
                .availableCreditLineAmount(new BigDecimal("5000.00"))
                .createdAt(java.time.LocalDateTime.now())
                .build();

        validLoan = Loan.builder()
                .id(loanId)
                .customer(validCustomer)
                .amount(new BigDecimal("1000.00"))
                .status(LoanStatus.ACTIVE)
                .commissionAmount(new BigDecimal("130.00"))
                .totalAmount(new BigDecimal("1130.00"))
                .paymentScheme("SCHEME_1")
                .interestRate(new BigDecimal("13.00"))
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    void createLoan_ValidRequest_ReturnsLoanResponse() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(loanRepository.save(any(Loan.class))).thenReturn(validLoan);
        when(installmentRepository.saveAll(any())).thenReturn(new ArrayList<>());
        doNothing().when(customerService).updateCustomerCreditLine(any(), any());

        // When
        LoanResponse response = loanService.createLoan(validLoanRequest);

        // Then
        assertNotNull(response);
        assertEquals(loanId, response.getId());
        assertEquals(customerId, response.getCustomerId());
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals(LoanStatus.ACTIVE, response.getStatus());
        verify(loanRepository).save(any(Loan.class));
        verify(installmentRepository).saveAll(any());
        verify(customerService).updateCustomerCreditLine(customerId, new BigDecimal("1000.00"));
    }

    @Test
    void createLoan_CustomerNotFound_ThrowsException() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(validLoanRequest);
        });
    }

    @Test
    void createLoan_InsufficientCredit_ThrowsException() {
        // Given
        validCustomer.setAvailableCreditLineAmount(new BigDecimal("500.00"));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(validLoanRequest);
        });
    }

    @Test
    void getLoanById_ValidId_ReturnsLoanResponse() {
        // Given
        List<Installment> installments = new ArrayList<>();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(validLoan));
        when(installmentRepository.findByLoanId(loanId)).thenReturn(installments);

        // When
        LoanResponse response = loanService.getLoanById(loanId);

        // Then
        assertNotNull(response);
        assertEquals(loanId, response.getId());
        assertEquals(customerId, response.getCustomerId());
        verify(loanRepository).findById(loanId);
        verify(installmentRepository).findByLoanId(loanId);
    }

    @Test
    void getLoanById_InvalidId_ThrowsException() {
        // Given
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            loanService.getLoanById(loanId);
        });
    }

    @Test
    void createLoan_Scheme1Customer_CalculatesCorrectCommission() {
        // Given
        validCustomer.setFirstName("Carlos"); // Starts with C
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(loanId);
            loan.calculatePaymentDetails();
            return loan;
        });
        when(installmentRepository.saveAll(any())).thenReturn(new ArrayList<>());
        doNothing().when(customerService).updateCustomerCreditLine(any(), any());

        // When
        LoanResponse response = loanService.createLoan(validLoanRequest);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("130.00").setScale(2), response.getPaymentPlan().getCommissionAmount().setScale(2));
    }

    @Test
    void createLoan_Scheme2Customer_CalculatesCorrectCommission() {
        // Given
        validCustomer.setFirstName("Pepe"); // Doesn't start with C, L, or H
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(loanId);
            loan.calculatePaymentDetails();
            return loan;
        });
        when(installmentRepository.saveAll(any())).thenReturn(new ArrayList<>());
        doNothing().when(customerService).updateCustomerCreditLine(any(), any());

        // When
        LoanResponse response = loanService.createLoan(validLoanRequest);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("160.00").setScale(2), response.getPaymentPlan().getCommissionAmount().setScale(2));
    }

    @Test
    void createLoan_CreatesFiveInstallments() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(loanRepository.save(any(Loan.class))).thenReturn(validLoan);
        when(installmentRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Installment> installments = invocation.getArgument(0);
            return installments;
        });
        doNothing().when(customerService).updateCustomerCreditLine(any(), any());

        // When
        LoanResponse response = loanService.createLoan(validLoanRequest);

        // Then
        assertNotNull(response);
        assertEquals(5, response.getPaymentPlan().getInstallments().size());
        verify(installmentRepository).saveAll(any());
    }
} 