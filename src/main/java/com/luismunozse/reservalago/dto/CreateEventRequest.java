package com.luismunozse.reservalago.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Solicitud para crear una reserva de tipo evento")
public record CreateEventRequest(
        @Schema(example = "Taller de fotografía", required = true)
        String titulo,

        @Schema(example = "2025-11-15T14:00:00Z", required = true, description = "Fecha y hora del evento en formato ISO 8601")
        Instant fechaISO,

        @Schema(example = "A", allowableValues = {"A", "B", "C", "D"})
        String circuito,

        @Schema(example = "30")
        Integer cupo,

        @Schema(example = "Evento especial para fotógrafos")
        String notas
) {
}
