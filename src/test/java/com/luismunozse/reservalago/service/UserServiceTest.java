package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateUserRequest;
import com.luismunozse.reservalago.dto.UpdateUserRequest;
import com.luismunozse.reservalago.dto.UserResponse;
import com.luismunozse.reservalago.model.User;
import com.luismunozse.reservalago.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFirstName("Admin");
        testUser.setLastName("Test");
        testUser.setEmail("admin@test.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("ADMIN");
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("Debe crear usuario correctamente")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Nuevo");
            request.setLastName("Usuario");
            request.setEmail("nuevo@test.com");
            request.setPassword("password123");
            request.setRole("ADMIN");

            when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getEmail()).isEqualTo("nuevo@test.com");
            assertThat(response.getRole()).isEqualTo("ADMIN");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Debe rechazar email duplicado")
        void shouldRejectDuplicateEmail() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("admin@test.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("email ya está registrado");
        }

        @Test
        @DisplayName("Debe usar rol ADMIN por defecto")
        void shouldUseDefaultRole() {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Test");
            request.setLastName("User");
            request.setEmail("test@test.com");
            request.setPassword("password123");
            request.setRole(null);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Debe rechazar rol inválido")
        void shouldRejectInvalidRole() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("test@test.com");
            request.setPassword("password123");
            request.setRole("INVALID_ROLE");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("rol debe ser ADMIN o MANAGER");
        }

        @Test
        @DisplayName("Debe aceptar rol MANAGER")
        void shouldAcceptManagerRole() {
            CreateUserRequest request = new CreateUserRequest();
            request.setFirstName("Manager");
            request.setLastName("User");
            request.setEmail("manager@test.com");
            request.setPassword("password123");
            request.setRole("MANAGER");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getRole()).isEqualTo("MANAGER");
        }
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("Debe retornar lista de usuarios")
        void shouldReturnUserList() {
            User user2 = new User();
            user2.setId(UUID.randomUUID());
            user2.setEmail("user2@test.com");
            user2.setRole("MANAGER");

            when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

            List<UserResponse> users = userService.getAllUsers();

            assertThat(users).hasSize(2);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay usuarios")
        void shouldReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> users = userService.getAllUsers();

            assertThat(users).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserByEmail()")
    class GetUserByEmail {

        @Test
        @DisplayName("Debe retornar usuario si existe")
        void shouldReturnUserIfExists() {
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));

            UserResponse response = userService.getUserByEmail("admin@test.com");

            assertThat(response.getEmail()).isEqualTo("admin@test.com");
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowIfNotExists() {
            when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("noexiste@test.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado");
        }
    }

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("Debe actualizar campos parcialmente")
        void shouldUpdateFieldsPartially() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setFirstName("NuevoNombre");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse response = userService.updateUser(testUser.getId(), request);

            assertThat(testUser.getFirstName()).isEqualTo("NuevoNombre");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Debe rechazar email duplicado en update")
        void shouldRejectDuplicateEmailOnUpdate() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("otro@test.com");

            User otroUser = new User();
            otroUser.setId(UUID.randomUUID());
            otroUser.setEmail("otro@test.com");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUser));

            assertThatThrownBy(() -> userService.updateUser(testUser.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("email ya está registrado");
        }

        @Test
        @DisplayName("Debe permitir mantener el mismo email")
        void shouldAllowSameEmail() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("admin@test.com");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse response = userService.updateUser(testUser.getId(), request);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar excepción si usuario no existe")
        void shouldThrowIfUserNotExists() {
            UUID randomId = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest();

            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(randomId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado");
        }

        @Test
        @DisplayName("Debe actualizar password encriptado")
        void shouldUpdateEncodedPassword() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setPassword("newPassword123");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(testUser.getId(), request);

            assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("Debe eliminar usuario si existe")
        void shouldDeleteUserIfExists() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            userService.deleteUser(testUser.getId());

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowIfNotExists() {
            UUID randomId = UUID.randomUUID();
            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(randomId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado");
        }
    }
}
