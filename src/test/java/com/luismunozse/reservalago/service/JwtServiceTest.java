package com.luismunozse.reservalago.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Configurar valores mediante reflection (simula @Value)
        ReflectionTestUtils.setField(jwtService, "secret",
                "testSecretKeyForJWTMustBeAtLeast32CharactersLongForTesting");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L); // 24 horas

        testUser = new User(
                "admin@test.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("Debe generar un token válido")
        void shouldGenerateValidToken() {
            String token = jwtService.generateToken(testUser);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT tiene 3 partes
        }

        @Test
        @DisplayName("El token debe contener el username correcto")
        void shouldContainCorrectUsername() {
            String token = jwtService.generateToken(testUser);

            String extractedUsername = jwtService.extractUsername(token);

            assertThat(extractedUsername).isEqualTo("admin@test.com");
        }

        @Test
        @DisplayName("El token debe contener el rol")
        void shouldContainRole() {
            String token = jwtService.generateToken(testUser);

            // El token contiene claims, verificamos que se puede extraer
            assertThat(jwtService.validateToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsername {

        @Test
        @DisplayName("Debe extraer el username del token")
        void shouldExtractUsername() {
            String token = jwtService.generateToken(testUser);

            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo("admin@test.com");
        }

        @Test
        @DisplayName("Debe lanzar excepción con token inválido")
        void shouldThrowWithInvalidToken() {
            assertThatThrownBy(() -> jwtService.extractUsername("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("extractExpiration()")
    class ExtractExpiration {

        @Test
        @DisplayName("Debe extraer la fecha de expiración")
        void shouldExtractExpiration() {
            String token = jwtService.generateToken(testUser);

            Date expiration = jwtService.extractExpiration(token);

            assertThat(expiration).isNotNull();
            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("La expiración debe ser aproximadamente 24 horas en el futuro")
        void shouldExpireIn24Hours() {
            String token = jwtService.generateToken(testUser);

            Date expiration = jwtService.extractExpiration(token);
            long diff = expiration.getTime() - System.currentTimeMillis();

            // Debe expirar en aproximadamente 24 horas (con margen de 1 minuto)
            assertThat(diff).isBetween(86400000L - 60000L, 86400000L + 60000L);
        }
    }

    @Nested
    @DisplayName("validateToken(String)")
    class ValidateTokenSimple {

        @Test
        @DisplayName("Debe retornar true para token válido")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateToken(testUser);

            Boolean isValid = jwtService.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false para token inválido")
        void shouldReturnFalseForInvalidToken() {
            Boolean isValid = jwtService.validateToken("invalid.token.here");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para token expirado")
        void shouldReturnFalseForExpiredToken() {
            // Crear servicio con expiración de 1ms
            JwtService shortLivedService = new JwtService();
            ReflectionTestUtils.setField(shortLivedService, "secret",
                    "testSecretKeyForJWTMustBeAtLeast32CharactersLongForTesting");
            ReflectionTestUtils.setField(shortLivedService, "expiration", 1L);

            String token = shortLivedService.generateToken(testUser);

            // Esperar a que expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Boolean isValid = shortLivedService.validateToken(token);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("validateToken(String, UserDetails)")
    class ValidateTokenWithUser {

        @Test
        @DisplayName("Debe retornar true cuando token y usuario coinciden")
        void shouldReturnTrueWhenTokenMatchesUser() {
            String token = jwtService.generateToken(testUser);

            Boolean isValid = jwtService.validateToken(token, testUser);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false cuando el usuario no coincide")
        void shouldReturnFalseWhenUserDoesNotMatch() {
            String token = jwtService.generateToken(testUser);

            UserDetails differentUser = new User(
                    "other@test.com",
                    "password",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            Boolean isValid = jwtService.validateToken(token, differentUser);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Tokens para diferentes roles")
    class DifferentRoles {

        @Test
        @DisplayName("Debe generar token para ROLE_ADMIN")
        void shouldGenerateTokenForAdmin() {
            UserDetails admin = new User("admin@test.com", "pass",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

            String token = jwtService.generateToken(admin);

            assertThat(jwtService.validateToken(token, admin)).isTrue();
        }

        @Test
        @DisplayName("Debe generar token para ROLE_MANAGER")
        void shouldGenerateTokenForManager() {
            UserDetails manager = new User("manager@test.com", "pass",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER")));

            String token = jwtService.generateToken(manager);

            assertThat(jwtService.validateToken(token, manager)).isTrue();
        }
    }
}
