package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.*;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationSummaryDTO(
        UUID id,
        LocalDate visitDate,
        int adults18Plus,
        int children2To17,
        int babiesLessThan2,
        int reducedMobility,
        int allergies,
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
                r.getAdults18Plus(),
                r.getChildren2To17(),
                r.getBabiesLessThan2(),
                r.getReducedMobility(),
                r.getAllergies(),
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
