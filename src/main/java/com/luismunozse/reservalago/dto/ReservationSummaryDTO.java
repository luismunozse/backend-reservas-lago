package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.*;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationSummaryDTO(
        UUID id,
        LocalDate visitDate,
        int adults14Plus,
        int minors,
        int reducedMobility,
        boolean allergies,
        String originLocation,
        HowHeard howHeard,
        ReservationStatus status,
        VisitorType visitorType,
        Circuit circuit,
        String institutionName,
        Integer institutionStudents
) {
    public static ReservationSummaryDTO from(Reservation r) {
        return new ReservationSummaryDTO(
                r.getId(),
                r.getVisitDate(),
                r.getAdults14Plus(),
                r.getMinors(),
                r.getReducedMobility(),
                r.isAllergies(),
                r.getOriginLocation(),
                r.getHowHeard(),
                r.getStatus(),
                r.getVisitorType(),
                r.getCircuit(),
                r.getInstitutionName(),
                r.getInstitutionStudents()
        );
    }

}
