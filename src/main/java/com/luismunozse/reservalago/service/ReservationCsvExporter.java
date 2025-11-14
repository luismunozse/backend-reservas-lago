package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Reservation;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ReservationCsvExporter {

    public byte[] exportCsv(List<Reservation> reservations, boolean maskContacts) {
        final String SEP = ",";
        final String LS = "\r\n";

        String header = String.join(SEP,
                "id","visit_date","first_name","last_name","dni","phone","email",
                "visitor_type","institution_name","institution_students",
                "adults_18_plus","children_2_to_17","babies_less_than_2","reduced_mobility","allergies",
                "origin_location","how_heard","status","created_at","updated_at");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("sep=").append(SEP).append(LS);
        sb.append(header).append(LS);

        for (Reservation r : reservations) {
            sb.append(String.join(SEP,
                    q(r.getId() != null ? r.getId().toString() : ""),
                    q(r.getVisitDate() != null ? r.getVisitDate().toString() : ""),
                    q(r.getFirstName()),
                    q(r.getLastName()),
                    q(maskContacts ? mask(r.getDni()) : n(r.getDni())),
                    q(maskContacts ? mask(r.getPhone()) : n(r.getPhone())),
                    q(maskContacts ? mask(r.getEmail()) : n(r.getEmail())),
                    q(r.getVisitorType() != null ? r.getVisitorType().name() : ""),
                    q(n(r.getInstitutionName())),
                    q(r.getInstitutionStudents() != null ? String.valueOf(r.getInstitutionStudents()) : ""),
                    q(String.valueOf(r.getAdults18Plus())),
                    q(String.valueOf(r.getChildren2To17())),
                    q(String.valueOf(r.getBabiesLessThan2())),
                    q(String.valueOf(r.getReducedMobility())),
                    q(String.valueOf(r.getAllergies())),
                    q(n(r.getOriginLocation())),
                    q(r.getHowHeard() != null ? r.getHowHeard().name() : ""),
                    q(r.getStatus() != null ? r.getStatus().name() : ""),
                    q(r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""),
                    q(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "")
            )).append(LS);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    private static String q(String s) {
        String v = n(s).replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private static String mask(String s) {
        if (s == null || s.length() < 4) return "***";
        int keep = Math.min(3, s.length());
        return "***" + s.substring(s.length() - keep);
    }
}

