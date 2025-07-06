package com.aplazo.backend.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testDoFilterInternal_WithValidToken() throws ServletException, IOException {
        // Given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        String validToken = "valid.jwt.token";
        UUID customerId = UUID.randomUUID();
        String role = "ROLE_CUSTOMER";
        
        request.addHeader("Authorization", "Bearer " + validToken);

        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getCustomerIdFromToken(validToken)).thenReturn(customerId);
        when(jwtTokenProvider.getRoleFromToken(validToken)).thenReturn(role);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getCustomerIdFromToken(validToken);
        verify(jwtTokenProvider).getRoleFromToken(validToken);
    }

    @Test
    void testDoFilterInternal_WithInvalidToken() throws ServletException, IOException {
        // Given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        String invalidToken = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + invalidToken);

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getCustomerIdFromToken(any());
        verify(jwtTokenProvider, never()).getRoleFromToken(any());
    }

    @Test
    void testDoFilterInternal_WithoutToken() throws ServletException, IOException {
        // Given
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(any());
        verify(jwtTokenProvider, never()).getCustomerIdFromToken(any());
        verify(jwtTokenProvider, never()).getRoleFromToken(any());
    }
} 