package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.LoginRequest;
import com.luismunozse.reservalago.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Debe retornar token con credenciales válidas")
        void shouldReturnTokenWithValidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@test.com");
            request.setPassword("password123");

            UserDetails userDetails = new User(
                    "admin@test.com",
                    "password123",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtService.generateToken(any())).thenReturn("mock-jwt-token");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.email").value("admin@test.com"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar 401 con credenciales inválidas")
        void shouldReturn401WithInvalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@test.com");
            request.setPassword("wrongpassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar 400 con email vacío")
        void shouldReturn400WithEmptyEmail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar 400 con password vacío")
        void shouldReturn400WithEmptyPassword() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@test.com");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar 400 con email inválido")
        void shouldReturn400WithInvalidEmail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("not-an-email");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar rol ROLE_MANAGER para manager")
        void shouldReturnManagerRole() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("manager@test.com");
            request.setPassword("password123");

            UserDetails userDetails = new User(
                    "manager@test.com",
                    "password123",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"))
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtService.generateToken(any())).thenReturn("manager-jwt-token");

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ROLE_MANAGER"));
        }
    }
}
