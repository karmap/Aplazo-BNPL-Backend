package com.aplazo.backend.service;

import com.aplazo.backend.dto.CustomerRequest;
import com.aplazo.backend.dto.CustomerResponse;
import com.aplazo.backend.entity.Customer;
import com.aplazo.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with first name: {}", request.getFirstName());

        // Parse date of birth
        LocalDate dateOfBirth = LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ISO_DATE);

        // Create customer entity
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .secondLastName(request.getSecondLastName())
                .dateOfBirth(dateOfBirth)
                .build();

        // Save customer (credit line will be calculated in @PrePersist)
        Customer savedCustomer = customerRepository.save(customer);

        log.info("Customer created successfully with ID: {}", savedCustomer.getId());

        return mapToResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");

        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID customerId) {
        log.info("Fetching customer with ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", customerId);
                    return new RuntimeException("Customer not found");
                });

        return mapToResponse(customer);
    }

    @Transactional
    public void updateCustomerCreditLine(UUID customerId, java.math.BigDecimal usedAmount) {
        log.info("Updating credit line for customer ID: {} with used amount: {}", customerId, usedAmount);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", customerId);
                    return new RuntimeException("Customer not found");
                });

        customer.updateAvailableCreditLine(usedAmount);
        customerRepository.save(customer);

        log.info("Credit line updated successfully for customer ID: {}", customerId);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setCreatedAt(customer.getCreatedAt());
        response.setCreditLineAmount(customer.getCreditLineAmount());
        response.setAvailableCreditLineAmount(customer.getAvailableCreditLineAmount());
        return response;
    }
} 