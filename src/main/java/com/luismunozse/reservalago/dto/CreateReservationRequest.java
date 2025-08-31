package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.Circuit;
import com.luismunozse.reservalago.model.HowHeard;
import com.luismunozse.reservalago.model.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Solicitud de creación de reserva")
//public record CreateReservationRequest(
//        @NotNull @FutureOrPresent LocalDate visitDate,
//        @NotBlank String firstName,
//        @NotBlank String lastName,
//        @NotBlank String dni,
//        @NotBlank String phone,
//        @Email @NotBlank String email,
//        @NotNull Circuit circuit,
//        @NotNull VisitorType visitorType,
//        String institutionName,
//        @Min(0) Integer institutionStudents,
//        @Min(1) int adults14Plus,
//        @Min(0) int minors,
//        @Min(0) int reducedMobility,
//        boolean allergies,
//        String comment,
//        @NotBlank String originLocation,
//        @NotNull HowHeard howHeard,
//        @AssertTrue boolean acceptedPolicies
//) {
//
//
//}
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
        @Schema(example = "1") int adults14Plus,
        @Schema(example = "0") int minors,
        @Schema(example = "0") int reducedMobility,
        @Schema(example = "false") boolean allergies,
        @Schema(example = "—") String comment,
        @Schema(example = "Córdoba, AR") String originLocation,
        @Schema(example = "ADS", allowableValues = {"SOCIAL","RECOMMENDATION","WEBSITE","ADS","OTHER"}) HowHeard howHeard,
        @Schema(example = "true") boolean acceptedPolicies
) {}
