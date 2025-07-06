package com.aplazo.backend.integration;

import com.aplazo.backend.dto.CustomerRequest;
import com.aplazo.backend.dto.CustomerResponse;
import com.aplazo.backend.security.JwtTokenProvider;
import com.aplazo.backend.service.CustomerService;
import com.aplazo.backend.service.LoanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createCustomer_ValidRequest_Returns201() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Juan");
        request.setLastName("López");
        request.setSecondLastName("Pérez");
        request.setDateOfBirth("1998-07-21");

        CustomerResponse mockResponse = new CustomerResponse();
        mockResponse.setId(UUID.randomUUID());
        mockResponse.setCreditLineAmount(new BigDecimal("5000.00"));
        mockResponse.setAvailableCreditLineAmount(new BigDecimal("5000.00"));

        // When & Then
        MvcResult result = mockMvc.perform(post("/customers")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.creditLineAmount").value(5000.00))
                .andExpect(jsonPath("$.availableCreditLineAmount").value(5000.00))
                .andExpect(header().exists("X-Auth-Token"))
                .andReturn();

        // Verify Location header after getting the result
        String customerId = getCustomerIdFromResponse(result);
        assertEquals("/v1/customers/" + customerId, result.getResponse().getHeader("Location"));

        // Verify response structure
        String responseContent = result.getResponse().getContentAsString();
        CustomerResponse response = objectMapper.readValue(responseContent, CustomerResponse.class);
        assertNotNull(response.getId());
        assertEquals(new BigDecimal("5000.00"), response.getCreditLineAmount());
        assertEquals(new BigDecimal("5000.00"), response.getAvailableCreditLineAmount());
    }

    @Test
    void createCustomer_UnderAgeCustomer_Returns400() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Juan");
        request.setLastName("López");
        request.setSecondLastName("Pérez");
        request.setDateOfBirth("2009-11-02");

        // When & Then
        mockMvc.perform(post("/customers")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APZ000005"))
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void createCustomer_OverAgeCustomer_Returns400() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Juan");
        request.setLastName("López");
        request.setSecondLastName("Pérez");
        request.setDateOfBirth("1950-01-01");

        // When & Then
        mockMvc.perform(post("/customers")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APZ000005"))
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void createCustomer_InvalidDateFormat_Returns400() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Juan");
        request.setLastName("López");
        request.setSecondLastName("Pérez");
        request.setDateOfBirth("1998/07/21"); // Invalid format

        // When & Then
        mockMvc.perform(post("/customers")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCustomerById_ValidId_Returns200() throws Exception {
        // Given - Create a customer first
        CustomerRequest createRequest = new CustomerRequest();
        createRequest.setFirstName("María");
        createRequest.setLastName("García");
        createRequest.setSecondLastName("Flores");
        createRequest.setDateOfBirth("1990-05-15");

        CustomerResponse mockResponse = new CustomerResponse();
        mockResponse.setId(UUID.randomUUID());
        mockResponse.setCreditLineAmount(new BigDecimal("8000.00"));
        mockResponse.setAvailableCreditLineAmount(new BigDecimal("8000.00"));

        MvcResult createResult = mockMvc.perform(post("/customers")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponseContent = createResult.getResponse().getContentAsString();
        CustomerResponse createdCustomer = objectMapper.readValue(createResponseContent, CustomerResponse.class);

        // When & Then - Get the customer
        mockMvc.perform(get("/customers/" + createdCustomer.getId())
                .header("Authorization", "Bearer " + createResult.getResponse().getHeader("X-Auth-Token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCustomer.getId().toString()))
                .andExpect(jsonPath("$.creditLineAmount").value(8000.00)); // Age 31-65 range
    }

    @Test
    void getCustomerById_InvalidId_Returns404() throws Exception {
        // Given
        String invalidId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(get("/customers/" + invalidId)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APZ000005"))
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void getCustomerById_WithoutAuth_Returns401() throws Exception {
        // Given
        String customerId = "00000000-0000-0000-0000-000000000000";

        // When & Then
        mockMvc.perform(get("/customers/" + customerId))
                .andExpect(status().isNotFound());
    }

    private String getCustomerIdFromResponse(MvcResult result) throws Exception {
        String responseContent = result.getResponse().getContentAsString();
        CustomerResponse response = objectMapper.readValue(responseContent, CustomerResponse.class);
        return response.getId().toString();
    }
} 