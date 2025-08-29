package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.Circuit;
import com.luismunozse.reservalago.model.HowHeard;
import com.luismunozse.reservalago.model.VisitorType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull @FutureOrPresent LocalDate visitDate,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String dni,
        @NotBlank String phone,
        @Email @NotBlank String email,
        @NotNull Circuit circuit,
        @NotNull VisitorType visitorType,
        String institutionName,
        @Min(0) Integer institutionStudents,
        @Min(1) int adults14Plus,
        @Min(0) int minors,
        @Min(0) int reducedMobility,
        boolean allergies,
        String comment,
        @NotBlank String originLocation,
        @NotNull HowHeard howHeard,
        @AssertTrue boolean acceptedPolicies
) {


}
