package com.luismunozse.reservalago.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Persona que asiste a la visita (visitante)")
public record VisitorDTO(
        @Schema(example = "Juan") String firstName,
        @Schema(example = "Perez") String lastName,
        @Pattern(regexp = "\\d{8}", message = "dni debe tener exactamente 8 digitos")
        @Schema(example = "12345678") String dni
) {}
