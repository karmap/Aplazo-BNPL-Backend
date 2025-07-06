package com.aplazo.backend.service;

import com.aplazo.backend.dto.CustomerRequest;
import com.aplazo.backend.dto.CustomerResponse;
import com.aplazo.backend.entity.Customer;
import com.aplazo.backend.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequest validCustomerRequest;
    private Customer validCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        
        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("Juan");
        validCustomerRequest.setLastName("López");
        validCustomerRequest.setSecondLastName("Pérez");
        validCustomerRequest.setDateOfBirth("1998-07-21");

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
    }

    @Test
    void createCustomer_ValidRequest_ReturnsCustomerResponse() {
        // Given
        when(customerRepository.save(any(Customer.class))).thenReturn(validCustomer);

        // When
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then
        assertNotNull(response);
        assertEquals(customerId, response.getId());
        assertEquals(new BigDecimal("5000.00"), response.getCreditLineAmount());
        assertEquals(new BigDecimal("5000.00"), response.getAvailableCreditLineAmount());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_UnderAgeCustomer_ThrowsException() {
        // Given
        validCustomerRequest.setDateOfBirth("2009-11-02");
        when(customerRepository.save(any(Customer.class))).thenThrow(new IllegalArgumentException("Customer age must be between 18 and 65 years"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer(validCustomerRequest);
        });
    }

    @Test
    void createCustomer_OverAgeCustomer_ThrowsException() {
        // Given
        validCustomerRequest.setDateOfBirth("1950-01-01");
        when(customerRepository.save(any(Customer.class))).thenThrow(new IllegalArgumentException("Customer age must be between 18 and 65 years"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer(validCustomerRequest);
        });
    }

    @Test
    void getCustomerById_ValidId_ReturnsCustomerResponse() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        // When
        CustomerResponse response = customerService.getCustomerById(customerId);

        // Then
        assertNotNull(response);
        assertEquals(customerId, response.getId());
        assertEquals(new BigDecimal("5000.00"), response.getCreditLineAmount());
    }

    @Test
    void getCustomerById_InvalidId_ThrowsException() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            customerService.getCustomerById(customerId);
        });
    }

    @Test
    void updateCustomerCreditLine_ValidAmount_UpdatesSuccessfully() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(validCustomer);

        // When
        customerService.updateCustomerCreditLine(customerId, new BigDecimal("1000.00"));

        // Then
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void updateCustomerCreditLine_ExcessiveAmount_ThrowsException() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            customerService.updateCustomerCreditLine(customerId, new BigDecimal("6000.00"));
        });
    }

    @Test
    void customerCreditLineCalculation_Age18To25_Returns3000() {
        // Given
        validCustomerRequest.setDateOfBirth("2005-01-01"); // 18 years old
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.calculateCreditLine();
            customer.setId(customerId);
            return customer;
        });

        // When
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then
        assertEquals(new BigDecimal("3000.00"), response.getCreditLineAmount());
    }

    @Test
    void customerCreditLineCalculation_Age26To30_Returns5000() {
        // Given
        validCustomerRequest.setDateOfBirth("1998-01-01"); // 25 years old
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.calculateCreditLine();
            customer.setId(customerId);
            return customer;
        });

        // When
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then
        assertEquals(new BigDecimal("5000.00"), response.getCreditLineAmount());
    }

    @Test
    void customerCreditLineCalculation_Age31To65_Returns8000() {
        // Given
        validCustomerRequest.setDateOfBirth("1980-01-01"); // 43 years old
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.calculateCreditLine();
            customer.setId(customerId);
            return customer;
        });

        // When
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then
        assertEquals(new BigDecimal("8000.00"), response.getCreditLineAmount());
    }
} 