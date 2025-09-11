package com.luismunozse.reservalago.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Fila para la grilla de administración de reservas")
public record AdminReservationDTO(
        UUID id,
        LocalDate visitDate,
        String firstName,
        String lastName,
        int adults14Plus,
        int minors,
        String email,
        String phone,
        String circuit,       // A/B/C/D
        String visitorType,   // INDIVIDUAL / EDUCATIONAL_INSTITUTION
        String originLocation,
        String status,        // PENDING / CONFIRMED / CANCELLED
        Instant createdAt
) {}

