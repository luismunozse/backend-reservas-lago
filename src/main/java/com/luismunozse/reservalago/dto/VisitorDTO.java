package com.luismunozse.reservalago.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persona que asiste a la visita (visitante)")
public record VisitorDTO(
        @Schema(example = "Juan") String firstName,
        @Schema(example = "Pérez") String lastName,
        @Schema(example = "12345678") String dni
) {}

