package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.Circuit;
import com.luismunozse.reservalago.model.HowHeard;
import com.luismunozse.reservalago.model.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Solicitud de creación de reserva")
public record CreateReservationRequest(
        @Schema(example = "2025-09-02") LocalDate visitDate,
        @Schema(example = "Luis") String firstName,
        @Schema(example = "Muñoz") String lastName,
        @Schema(example = "12345678") String dni,
        @Schema(example = "+54 9 351 000-0000") String phone,
        @Schema(example = "luis@example.com") String email,
        @Schema(example = "A", allowableValues = {"A","B","C","D"}) Circuit circuit,
        @Schema(example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL","EDUCATIONAL_INSTITUTION"}) VisitorType visitorType,
        @Schema(example = "Escuela Técnica N°1") String institutionName,
        @Schema(example = "25") Integer institutionStudents,
        @Schema(example = "1") int adults18Plus,
        @Schema(example = "0") int children2To17,
        @Schema(example = "0") int babiesLessThan2,
        @Schema(example = "0") int reducedMobility,
        @Schema(example = "0") int allergies,
        @Schema(example = "—") String comment,
        @Schema(example = "Córdoba, AR") String originLocation,
        @Schema(example = "ADS", allowableValues = {"SOCIAL","RECOMMENDATION","WEBSITE","ADS","OTHER"}) HowHeard howHeard,
        @Schema(example = "true") boolean acceptedPolicies
) {}
