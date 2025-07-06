package com.aplazo.backend.service;

import com.aplazo.backend.dto.LoanRequest;
import com.aplazo.backend.dto.LoanResponse;
import com.aplazo.backend.entity.Customer;
import com.aplazo.backend.entity.Installment;
import com.aplazo.backend.entity.InstallmentStatus;
import com.aplazo.backend.entity.Loan;
import com.aplazo.backend.repository.CustomerRepository;
import com.aplazo.backend.repository.InstallmentRepository;
import com.aplazo.backend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final InstallmentRepository installmentRepository;
    private final CustomerService customerService;

    @Transactional
    public LoanResponse createLoan(LoanRequest request) {
        log.info("Creating loan for customer ID: {} with amount: {}", request.getCustomerId(), request.getAmount());

        // Get customer and validate credit line
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", request.getCustomerId());
                    return new RuntimeException("Customer not found");
                });

        if (!customer.hasSufficientCredit(request.getAmount())) {
            log.error("Insufficient credit line for customer ID: {}", request.getCustomerId());
            throw new RuntimeException("Insufficient credit line available");
        }

        // Create loan
        Loan loan = Loan.builder()
                .customer(customer)
                .amount(request.getAmount())
                .paymentScheme(customer.getPaymentScheme())
                .build();

        // Save loan
        Loan savedLoan = loanRepository.save(loan);

        // Create installments
        List<Installment> installments = createInstallments(savedLoan);
        savedLoan.setInstallments(installments);

        // Update customer credit line
        customerService.updateCustomerCreditLine(request.getCustomerId(), request.getAmount());

        log.info("Loan created successfully with ID: {}", savedLoan.getId());

        return mapToResponse(savedLoan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(UUID loanId) {
        log.info("Fetching loan with ID: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> {
                    log.error("Loan not found with ID: {}", loanId);
                    return new RuntimeException("Loan not found");
                });

        // Load installments
        List<Installment> installments = installmentRepository.findByLoanId(loanId);
        loan.setInstallments(installments);

        return mapToResponse(loan);
    }

    private List<Installment> createInstallments(Loan loan) {
        List<Installment> installments = new ArrayList<>();
        BigDecimal installmentAmount = loan.getTotalAmount().divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
        LocalDate startDate = LocalDate.now().plusWeeks(2); // First payment in 2 weeks

        for (int i = 0; i < 5; i++) {
            Installment installment = Installment.builder()
                    .loan(loan)
                    .amount(installmentAmount)
                    .scheduledPaymentDate(startDate.plusWeeks(i * 2)) // Biweekly payments
                    .installmentNumber(i + 1)
                    .status(i == 0 ? InstallmentStatus.NEXT : InstallmentStatus.PENDING)
                    .build();

            installments.add(installment);
        }

        return installmentRepository.saveAll(installments);
    }

    private LoanResponse mapToResponse(Loan loan) {
        LoanResponse response = new LoanResponse();
        response.setId(loan.getId());
        response.setCustomerId(loan.getCustomer().getId());
        response.setAmount(loan.getAmount());
        response.setStatus(loan.getStatus());
        response.setCreatedAt(loan.getCreatedAt());

        // Create payment plan
        LoanResponse.PaymentPlan paymentPlan = new LoanResponse.PaymentPlan();
        paymentPlan.setCommissionAmount(loan.getCommissionAmount());

        List<LoanResponse.InstallmentResponse> installmentResponses = new ArrayList<>();
        if (loan.getInstallments() != null) {
            for (Installment installment : loan.getInstallments()) {
                LoanResponse.InstallmentResponse installmentResponse = new LoanResponse.InstallmentResponse();
                installmentResponse.setAmount(installment.getAmount());
                installmentResponse.setScheduledPaymentDate(installment.getScheduledPaymentDate().toString());
                installmentResponse.setStatus(installment.getStatus().name());
                installmentResponses.add(installmentResponse);
            }
        }
        paymentPlan.setInstallments(installmentResponses);

        response.setPaymentPlan(paymentPlan);
        return response;
    }
} 