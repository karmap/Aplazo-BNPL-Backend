package com.aplazo.backend.dto;

import lombok.Data;

@Data
public class ErrorResponse {

    private String code;

    private String error;

    private Long timestamp;

    private String message;

    private String path;
} 