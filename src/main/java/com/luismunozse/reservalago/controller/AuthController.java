package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.LoginRequest;
import com.luismunozse.reservalago.dto.LoginResponse;
import com.luismunozse.reservalago.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Operation(summary = "Login de usuario admin", description = "Autenticación con email y contraseña, retorna JWT token")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Autenticar con email/password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Generar token JWT
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            // Extraer rol
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("ROLE_USER");

            // Retornar respuesta con token
            LoginResponse response = new LoginResponse(token, userDetails.getUsername(), role);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("Credenciales inválidas"));
        } catch (AuthenticationException e) {
            // Cubre UsernameNotFoundException, InternalAuthenticationServiceException, etc.
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("Credenciales inválidas"));
        } catch (Exception e) {
            log.error("Error inesperado en login para usuario {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Error en el servidor"));
        }
    }

    // Clase interna para respuestas de error
    private record ErrorResponse(String message) {}
}



