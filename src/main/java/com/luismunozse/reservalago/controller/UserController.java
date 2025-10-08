package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateUserRequest;
import com.luismunozse.reservalago.dto.UpdateUserRequest;
import com.luismunozse.reservalago.dto.UserResponse;
import com.luismunozse.reservalago.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Gestión de Usuarios", description = "Endpoints para administrar usuarios del sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Crear nuevo usuario administrador", 
               description = "Crea un nuevo usuario con rol de administrador en el sistema")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios", 
               description = "Obtiene la lista completa de usuarios del sistema")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{email}")
    @Operation(summary = "Obtener usuario por email", 
               description = "Obtiene los detalles de un usuario específico por su email")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        try {
            UserResponse user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar usuario", 
               description = "Actualiza los datos de un usuario existente. Solo se actualizan los campos enviados.")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse response = userService.updateUser(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Eliminar usuario", 
               description = "Elimina un usuario del sistema de forma permanente")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
