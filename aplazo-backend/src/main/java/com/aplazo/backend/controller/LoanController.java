package com.aplazo.backend.controller;

import com.aplazo.backend.dto.LoanRequest;
import com.aplazo.backend.dto.LoanResponse;
import com.aplazo.backend.dto.ErrorResponse;
import com.aplazo.backend.service.LoanService;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loans", description = "Manage loans")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Create a loan", description = "Creates a new loan for a customer")
    @SecurityRequirement(name = "aplazoAuth")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Loan creation data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = LoanRequest.class),
            examples = {
                @ExampleObject(
                    name = "Small Loan",
                    summary = "Small loan amount - $300",
                    value = """
                        {
                          "customerId": "CUSTOMER_UUID_HERE",
                          "amount": 300.00,
                          "paymentScheme": "WEEKLY"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Medium Loan",
                    summary = "Medium loan amount - $800",
                    value = """
                        {
                          "customerId": "CUSTOMER_UUID_HERE",
                          "amount": 800.00,
                          "paymentScheme": "WEEKLY"
                        }
                        """
                ),
                @ExampleObject(
                    name = "Large Loan",
                    summary = "Large loan amount - $1500",
                    value = """
                        {
                          "customerId": "CUSTOMER_UUID_HERE",
                          "amount": 1500.00,
                          "paymentScheme": "WEEKLY"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)),
            headers = {
                @io.swagger.v3.oas.annotations.headers.Header(name = "Location", description = "Relative path to search for newly created loan")
            }),
        @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"code\":\"APZ000006\",\"error\":\"INVALID_LOAN_REQUEST\",\"timestamp\":1739397485,\"message\":\"Error detail\",\"path\":\"/v1/loans\"}"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody LoanRequest request) {
        log.info("Received request to create loan for customer ID: {} with amount: {}", request.getCustomerId(), request.getAmount());

        LoanResponse response = loanService.createLoan(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/v1/loans/" + response.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(response);
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan by ID", description = "Retrieves loan information by loan ID")
    @SecurityRequirement(name = "aplazoAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = LoanResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"code\":\"APZ000008\",\"error\":\"LOAN_NOT_FOUND\",\"timestamp\":1739397485,\"message\":\"Error detail\",\"path\":\"/v1/loans/{loanId}\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable UUID loanId) {
        log.info("Received request to get loan with ID: {}", loanId);

        LoanResponse response = loanService.getLoanById(loanId);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);

        String errorCode = "APZ000008";
        String errorType = "LOAN_NOT_FOUND";
        
        if (ex.getMessage().contains("Customer not found")) {
            errorCode = "APZ000005";
            errorType = "CUSTOMER_NOT_FOUND";
        } else if (ex.getMessage().contains("Insufficient credit line")) {
            errorCode = "APZ000006";
            errorType = "INVALID_LOAN_REQUEST";
        }

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(errorCode);
        errorResponse.setError(errorType);
        errorResponse.setTimestamp(Instant.now().getEpochSecond());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Illegal argument exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("APZ000006");
        errorResponse.setError("INVALID_LOAN_REQUEST");
        errorResponse.setTimestamp(Instant.now().getEpochSecond());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
} 