package com.aplazo.backend.integration;

import com.aplazo.backend.dto.CustomerRequest;
import com.aplazo.backend.dto.CustomerResponse;
import com.aplazo.backend.dto.LoanRequest;
import com.aplazo.backend.dto.LoanResponse;
import com.aplazo.backend.entity.LoanStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID validCustomerId;
    private UUID invalidCustomerId;

    @BeforeEach
    void setUp() throws Exception {
        // Create a valid customer for testing
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Test");
        customerRequest.setLastName("Customer");
        customerRequest.setSecondLastName("Loan");
        customerRequest.setDateOfBirth("1990-01-01");

        MvcResult result = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CustomerResponse customerResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), CustomerResponse.class);
        validCustomerId = customerResponse.getId();
        invalidCustomerId = UUID.randomUUID();
    }

    @Test
    void createLoan_ValidRequest_Returns201() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(validCustomerId);
        request.setAmount(new BigDecimal("500.00"));

        // When & Then
        MvcResult result = mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/v1/loans/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(validCustomerId.toString()))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.status").value(LoanStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.paymentPlan").exists())
                .andExpect(jsonPath("$.paymentPlan.commissionAmount").exists())
                .andExpect(jsonPath("$.paymentPlan.installments").isArray())
                .andExpect(jsonPath("$.paymentPlan.installments").value(org.hamcrest.Matchers.hasSize(5)))
                .andReturn();

        // Verify the response structure
        LoanResponse loanResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoanResponse.class);
        
        // Verify installments have correct structure
        mockMvc.perform(get("/loans/" + loanResponse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanResponse.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(validCustomerId.toString()))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    void createLoan_InvalidCustomerId_Returns400() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(invalidCustomerId);
        request.setAmount(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000005"))
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Customer not found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/loans"));
    }

    @Test
    void createLoan_AmountExceedsCreditLine_Returns400() throws Exception {
        // Given - Create a loan that exceeds the customer's credit line
        LoanRequest request = new LoanRequest();
        request.setCustomerId(validCustomerId);
        request.setAmount(new BigDecimal("10000.00")); // Exceeds credit line

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000006"))
                .andExpect(jsonPath("$.error").value("INVALID_LOAN_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient credit line")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/loans"));
    }

    @Test
    void createLoan_ZeroAmount_Returns400() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(validCustomerId);
        request.setAmount(BigDecimal.ZERO);

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLoan_NullAmount_Returns400() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(validCustomerId);
        request.setAmount(null);

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLoan_NullCustomerId_Returns400() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(null);
        request.setAmount(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLoan_NegativeAmount_Returns400() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(validCustomerId);
        request.setAmount(new BigDecimal("-100.00"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLoanById_ValidId_Returns200() throws Exception {
        // Given - Create a loan first
        LoanRequest createRequest = new LoanRequest();
        createRequest.setCustomerId(validCustomerId);
        createRequest.setAmount(new BigDecimal("300.00"));

        MvcResult createResult = mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse createdLoan = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), LoanResponse.class);

        // When & Then - Get the loan
        mockMvc.perform(get("/loans/" + createdLoan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdLoan.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(validCustomerId.toString()))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.status").value(LoanStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.paymentPlan").exists())
                .andExpect(jsonPath("$.paymentPlan.installments").isArray())
                .andExpect(jsonPath("$.paymentPlan.installments").value(org.hamcrest.Matchers.hasSize(5)));
    }

    @Test
    void getLoanById_InvalidId_Returns404() throws Exception {
        // Given
        UUID invalidLoanId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/loans/" + invalidLoanId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000008"))
                .andExpect(jsonPath("$.error").value("LOAN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Loan not found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/loans/" + invalidLoanId));
    }

    @Test
    void getLoanById_InvalidUUIDFormat_Returns400() throws Exception {
        // Given
        String invalidUUID = "invalid-uuid";

        // When & Then
        mockMvc.perform(get("/loans/" + invalidUUID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLoan_MultipleLoansForSameCustomer_Returns201() throws Exception {
        // Given - Create multiple loans for the same customer
        LoanRequest request1 = new LoanRequest();
        request1.setCustomerId(validCustomerId);
        request1.setAmount(new BigDecimal("200.00"));

        LoanRequest request2 = new LoanRequest();
        request2.setCustomerId(validCustomerId);
        request2.setAmount(new BigDecimal("300.00"));

        // When & Then - Both loans should be created successfully
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(validCustomerId.toString()))
                .andExpect(jsonPath("$.amount").value(200.00));

        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(validCustomerId.toString()))
                .andExpect(jsonPath("$.amount").value(300.00));
    }

    @Test
    void createLoan_DifferentAmounts_Returns201() throws Exception {
        // Test different loan amounts
        BigDecimal[] amounts = {
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1000.00")
        };

        for (BigDecimal amount : amounts) {
            LoanRequest request = new LoanRequest();
            request.setCustomerId(validCustomerId);
            request.setAmount(amount);

            mockMvc.perform(post("/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                    .andExpect(jsonPath("$.paymentPlan.installments").value(org.hamcrest.Matchers.hasSize(5)));
        }
    }

    @Test
    void createLoan_EmptyRequestBody_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLoan_MalformedJSON_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"invalid-uuid\",\"amount\":100}"))
                .andExpect(status().isBadRequest());
    }
} 