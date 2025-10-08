package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String firstName;
    
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String lastName;
    
    @Email(message = "El email debe ser válido")
    private String email;
    
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    
    private Boolean enabled;
}
