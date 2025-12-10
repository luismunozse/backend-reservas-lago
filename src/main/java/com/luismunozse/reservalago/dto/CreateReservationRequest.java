package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.Circuit;
import com.luismunozse.reservalago.model.HowHeard;
import com.luismunozse.reservalago.model.VisitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Solicitud de creación de reserva")
public record CreateReservationRequest(
        @NotNull @Schema(example = "2025-09-02") LocalDate visitDate,
        @NotBlank @Schema(example = "Luis") String firstName,
        @NotBlank @Schema(example = "Muñoz") String lastName,
        @NotBlank
        @Pattern(regexp = "\\d{8}", message = "dni debe tener exactamente 8 digitos")
        @Schema(example = "12345678") String dni,
        @NotBlank @Schema(example = "+54 9 351 000-0000") String phone,
        @NotBlank @Email @Schema(example = "luis@example.com") String email,
        @NotNull @Schema(example = "A", allowableValues = {"A","B","C","D"}) Circuit circuit,
        @NotNull @Schema(example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL","EDUCATIONAL_INSTITUTION", "EVENT"}) VisitorType visitorType,
        @Schema(example = "Escuela Técnica N°1") String institutionName,
        @Schema(example = "25") Integer institutionStudents,
        @Min(0) @Schema(example = "1") int adults18Plus,
        @Min(0) @Schema(example = "0") int children2To17,
        @Min(0) @Schema(example = "0") int babiesLessThan2,
        @Min(0) @Schema(example = "0") int reducedMobility,
        @Schema(example = "—") String comment,
        @Schema(example = "Córdoba, AR") String originLocation,
        @NotNull @Schema(example = "ADS", allowableValues = {"SOCIAL","RECOMMENDATION","WEBSITE","ADS","OTHER"}) HowHeard howHeard,
        @AssertTrue @Schema(example = "true") boolean acceptedPolicies,

        @Schema(description = "Listado de visitantes efectivamente presentes")
        java.util.List<VisitorDTO> visitors
) {}
