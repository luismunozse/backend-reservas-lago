package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.CreateUserRequest;
import com.luismunozse.reservalago.dto.UpdateUserRequest;
import com.luismunozse.reservalago.dto.UserResponse;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse createTestUserResponse() {
        return UserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("Test")
                .email("admin@test.com")
                .role("ADMIN")
                .phone("1155667788")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/admin/users")
    class CreateUser {

        @Test
        @DisplayName("Debe crear usuario correctamente")
        void shouldCreateUserSuccessfully() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Nuevo");
            request.setLastName("Usuario");
            request.setEmail("nuevo@test.com");
            request.setPassword("password123");
            request.setRole("ADMIN");

            UserResponse response = createTestUserResponse();
            response.setEmail("nuevo@test.com");
            response.setFirstName("Nuevo");
            response.setLastName("Usuario");

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("nuevo@test.com"))
                    .andExpect(jsonPath("$.firstName").value("Nuevo"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            verify(userService).createUser(any(CreateUserRequest.class));
        }

        @Test
        @DisplayName("Debe rechazar email inválido")
        void shouldRejectInvalidEmail() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Test");
            request.setLastName("User");
            request.setEmail("invalid-email");
            request.setPassword("password123");

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe rechazar contraseña corta")
        void shouldRejectShortPassword() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Test");
            request.setLastName("User");
            request.setEmail("test@test.com");
            request.setPassword("123");

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe rechazar nombre vacío")
        void shouldRejectEmptyFirstName() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("");
            request.setLastName("User");
            request.setEmail("test@test.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe rechazar rol inválido")
        void shouldRejectInvalidRole() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Test");
            request.setLastName("User");
            request.setEmail("test@test.com");
            request.setPassword("password123");
            request.setRole("INVALID");

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe manejar email duplicado")
        void shouldHandleDuplicateEmail() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Test");
            request.setLastName("User");
            request.setEmail("existing@test.com");
            request.setPassword("password123");

            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado"));

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Debe crear usuario con rol MANAGER")
        void shouldCreateUserWithManagerRole() throws Exception {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Manager");
            request.setLastName("User");
            request.setEmail("manager@test.com");
            request.setPassword("password123");
            request.setRole("MANAGER");

            UserResponse response = createTestUserResponse();
            response.setRole("MANAGER");

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("MANAGER"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetAllUsers {

        @Test
        @DisplayName("Debe retornar lista de usuarios")
        void shouldReturnUserList() throws Exception {
            UserResponse user1 = createTestUserResponse();
            UserResponse user2 = createTestUserResponse();
            user2.setEmail("manager@test.com");
            user2.setRole("MANAGER");

            when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].email").value("admin@test.com"))
                    .andExpect(jsonPath("$[1].email").value("manager@test.com"));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay usuarios")
        void shouldReturnEmptyList() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{email}")
    class GetUserByEmail {

        @Test
        @DisplayName("Debe retornar usuario si existe")
        void shouldReturnUserIfExists() throws Exception {
            UserResponse response = createTestUserResponse();

            when(userService.getUserByEmail("admin@test.com")).thenReturn(response);

            mockMvc.perform(get("/api/admin/users/{email}", "admin@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("admin@test.com"))
                    .andExpect(jsonPath("$.firstName").value("Admin"));
        }

        @Test
        @DisplayName("Debe retornar 404 si usuario no existe")
        void shouldReturn404IfNotExists() throws Exception {
            when(userService.getUserByEmail("noexiste@test.com"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            mockMvc.perform(get("/api/admin/users/{email}", "noexiste@test.com"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{userId}")
    class UpdateUser {

        @Test
        @DisplayName("Debe actualizar usuario correctamente")
        void shouldUpdateUserSuccessfully() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("NuevoNombre");

            UserResponse response = createTestUserResponse();
            response.setId(userId);
            response.setFirstName("NuevoNombre");

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("NuevoNombre"));

            verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
        }

        @Test
        @DisplayName("Debe actualizar múltiples campos")
        void shouldUpdateMultipleFields() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("NuevoNombre");
            request.setLastName("NuevoApellido");
            request.setPhone("1199887766");

            UserResponse response = createTestUserResponse();
            response.setId(userId);
            response.setFirstName("NuevoNombre");
            response.setLastName("NuevoApellido");
            response.setPhone("1199887766");

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("NuevoNombre"))
                    .andExpect(jsonPath("$.lastName").value("NuevoApellido"))
                    .andExpect(jsonPath("$.phone").value("1199887766"));
        }

        @Test
        @DisplayName("Debe retornar 404 si usuario no existe")
        void shouldReturn404IfNotExists() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("Test");

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Debe rechazar email inválido en update")
        void shouldRejectInvalidEmailOnUpdate() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("invalid-email");

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe manejar email duplicado en update")
        void shouldHandleDuplicateEmailOnUpdate() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("existing@test.com");

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado"));

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Debe actualizar estado enabled")
        void shouldUpdateEnabledStatus() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEnabled(false);

            UserResponse response = createTestUserResponse();
            response.setId(userId);
            response.setEnabled(false);

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("Debe actualizar rol de usuario")
        void shouldUpdateUserRole() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRole("MANAGER");

            UserResponse response = createTestUserResponse();
            response.setId(userId);
            response.setRole("MANAGER");

            when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/admin/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("MANAGER"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/users/{userId}")
    class DeleteUser {

        @Test
        @DisplayName("Debe eliminar usuario correctamente")
        void shouldDeleteUserSuccessfully() throws Exception {
            UUID userId = UUID.randomUUID();
            doNothing().when(userService).deleteUser(userId);

            mockMvc.perform(delete("/api/admin/users/{userId}", userId))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(userId);
        }

        @Test
        @DisplayName("Debe retornar 404 si usuario no existe")
        void shouldReturn404IfNotExists() throws Exception {
            UUID userId = UUID.randomUUID();
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"))
                    .when(userService).deleteUser(userId);

            mockMvc.perform(delete("/api/admin/users/{userId}", userId))
                    .andExpect(status().isNotFound());
        }
    }
}
