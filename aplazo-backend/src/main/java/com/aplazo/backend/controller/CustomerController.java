package com.aplazo.backend.controller;

import com.aplazo.backend.dto.CustomerRequest;
import com.aplazo.backend.dto.CustomerResponse;
import com.aplazo.backend.dto.ErrorResponse;
import com.aplazo.backend.security.JwtTokenProvider;
import com.aplazo.backend.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customers", description = "Manage customers")
public class CustomerController {

    private final CustomerService customerService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @Operation(summary = "Create a customer", description = "Creates a new customer with credit line assignment")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Customer registration data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = CustomerRequest.class),
            examples = {
                @ExampleObject(
                    name = "Young Customer (Age 22)",
                    summary = "Customer aged 22 - gets $3,000 credit line",
                    value = """
                        {
                          "firstName": "Alice",
                          "lastName": "Johnson",
                          "secondLastName": "Brown",
                          "dateOfBirth": "2002-06-15"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Adult Customer (Age 30)",
                    summary = "Customer aged 30 - gets $5,000 credit line",
                    value = """
                        {
                          "firstName": "Bob",
                          "lastName": "Wilson",
                          "secondLastName": "Davis",
                          "dateOfBirth": "1994-03-20"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Senior Customer (Age 40)",
                    summary = "Customer aged 40 - gets $8,000 credit line",
                    value = """
                        {
                          "firstName": "Carol",
                          "lastName": "Smith",
                          "secondLastName": "Miller",
                          "dateOfBirth": "1984-11-10"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class)),
            headers = {
                @io.swagger.v3.oas.annotations.headers.Header(name = "Location", description = "Relative path to search for newly created customer"),
                @io.swagger.v3.oas.annotations.headers.Header(name = "X-Auth-Token", description = "JWT with the required roles")
            }),
        @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"code\":\"APZ000002\",\"error\":\"INVALID_CUSTOMER_REQUEST\",\"timestamp\":1739397485,\"message\":\"Error detail\",\"path\":\"/v1/customers\"}"))),
        @ApiResponse(responseCode = "429", description = "Too many requests",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"code\":\"APZ000003\",\"error\":\"RATE_LIMIT_ERROR\",\"timestamp\":1739397485,\"message\":\"Error detail\",\"path\":\"/v1/customers\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("Received request to create customer: {}", request.getFirstName());

        CustomerResponse response = customerService.createCustomer(request);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(response.getId(), "ROLE_CUSTOMER");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/v1/customers/" + response.getId());
        headers.add("X-Auth-Token", token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieves a list of all registered customers")
    @SecurityRequirement(name = "aplazoAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        log.info("Received request to get all customers");

        List<CustomerResponse> customers = customerService.getAllCustomers();

        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer information by customer ID")
    @SecurityRequirement(name = "aplazoAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"code\":\"APZ000005\",\"error\":\"CUSTOMER_NOT_FOUND\",\"timestamp\":1739397485,\"message\":\"Error detail\",\"path\":\"/v1/customers/{customerId}\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID customerId) {
        log.info("Received request to get customer with ID: {}", customerId);

        CustomerResponse response = customerService.getCustomerById(customerId);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("APZ000005");
        errorResponse.setError("CUSTOMER_NOT_FOUND");
        errorResponse.setTimestamp(Instant.now().getEpochSecond());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Illegal argument exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("APZ000002");
        errorResponse.setError("INVALID_CUSTOMER_REQUEST");
        errorResponse.setTimestamp(Instant.now().getEpochSecond());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
} 