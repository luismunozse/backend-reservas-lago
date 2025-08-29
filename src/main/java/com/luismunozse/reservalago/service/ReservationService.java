package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.VisitorType;
import com.luismunozse.reservalago.repo.AvailabilityRuleRepository;
import com.luismunozse.reservalago.repo.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservations;
    private final AvailabilityRuleRepository availability;
    private final JavaMailSender mailSender;

    @Value("${app.defaultCapacity:30}")
    private int defaultCapacity;

    public Map<String, Object> availabilityFor(LocalDate day) {
        int capacity = availability.findByDay(day).map(AvailabilityRule::getCapacity)
                .orElse(defaultCapacity);
        int used = reservations.totalPeopleForDate(day);
        int remaining = Math.max(capacity - used, 0);
        Map<String, Object> map = new HashMap<>();
        map.put("date", day);
        map.put("capacity", capacity);
        map.put("remaining", remaining);
        return map;
    }

    @Transactional
    public UUID create(CreateReservationRequest req) {
        int capacity = (int) availabilityFor(req.visitDate()).get("capacity");
        int used = reservations.totalPeopleForDate(req.visitDate());
        int requested = req.adults14Plus() + req.minors();
        if (used + requested > capacity) {
            throw new IllegalStateException("No hay cupo para esa fecha");
        }
        if (req.visitorType() == VisitorType.EDUCATIONAL_INSTITUTION) {
            if (req.institutionName() == null || req.institutionName().isBlank()) {
                throw new IllegalArgumentException("Para instituciones, 'institutionName' es obligatorio");
            }
        }

        Reservation r = new Reservation();
        r.setVisitDate(req.visitDate());
        r.setFirstName(req.firstName());
        r.setLastName(req.lastName());
        r.setDni(req.dni());
        r.setPhone(req.phone());
        r.setEmail(req.email());
        r.setCircuit(req.circuit());
        r.setVisitorType(req.visitorType());
        r.setInstitutionName(req.institutionName());
        r.setInstitutionStudents(req.institutionStudents());
        r.setAdults14Plus(req.adults14Plus());
        r.setMinors(req.minors());
        r.setReducedMobility(req.reducedMobility());
        r.setAllergies(req.allergies());
        r.setComment(req.comment());
        r.setOriginLocation(req.originLocation());
        r.setHowHeard(req.howHeard());
        r.setAcceptedPolicies(req.acceptedPolicies());
        r.setStatus(ReservationStatus.PENDING);

        reservations.save(r);

        sendEmail(r.getEmail(), "Reserva recibida", "Tu reserva fue registrada. Te recordaremos 48h antes.");
        return r.getId();
    }

    @Scheduled(cron = "0 0 * * * *") // cada hora
    public void sendReminders() {
        LocalDate in48h = LocalDate.now().plusDays(2);
        List<Reservation> list = reservations.findAllByVisitDateAndStatus(in48h, ReservationStatus.PENDING);
        for (Reservation r : list) {
            sendEmail(r.getEmail(), "Recordatorio de visita", "¡Te esperamos! Llegá 15 min antes…");
        }
    }


    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception ignored) { }
    }

    // ==== Exportación CSV ====
//    public byte[] exportCsv(LocalDate date, ReservationStatus status, VisitorType visitorType, boolean maskContacts) {
//        List<Reservation> list = reservations.findAllByVisitDate(date);
//        if (status != null) {
//            list = list.stream().filter(r -> r.getStatus() == status).collect(Collectors.toList());
//        }
//        if (visitorType != null) {
//            list = list.stream().filter(r -> r.getVisitorType() == visitorType).collect(Collectors.toList());
//        }
//        String header = String.join(",",
//                "id","visit_date","first_name","last_name","dni","phone","email",
//                "circuit","visitor_type","institution_name","institution_students",
//                "adults_14_plus","minors","reduced_mobility","allergies",
//                "origin_location","how_heard","status","created_at","updated_at");
//
//
//        String rows = list.stream().map(r -> String.join(",",
//                q(r.getId() != null ? r.getId().toString() : ""),
//                q(r.getVisitDate() != null ? r.getVisitDate().toString() : ""),
//                q(r.getFirstName()),
//                q(r.getLastName()),
//                q(maskContacts ? mask(r.getDni()) : n(r.getDni())),
//                q(maskContacts ? mask(r.getPhone()) : n(r.getPhone())),
//                q(maskContacts ? mask(r.getEmail()) : n(r.getEmail())),
//                q(r.getCircuit() != null ? r.getCircuit().name() : ""),
//                q(r.getVisitorType() != null ? r.getVisitorType().name() : ""),
//                q(n(r.getInstitutionName())),
//                q(r.getInstitutionStudents() != null ? String.valueOf(r.getInstitutionStudents()) : ""),
//                q(String.valueOf(r.getAdults14Plus())),
//                q(String.valueOf(r.getMinors())),
//                q(String.valueOf(r.getReducedMobility())),
//                q(String.valueOf(r.isAllergies())),
//                q(n(r.getOriginLocation())),
//                q(r.getHowHeard() != null ? r.getHowHeard().name() : ""),
//                q(r.getStatus() != null ? r.getStatus().name() : ""),
//                q(r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""),
//                q(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : ""))).collect(Collectors.joining(""));
//
//        String csv = "•" + header + " " + rows + (rows.isEmpty() ? "" : " ");
//        return csv.getBytes(StandardCharsets.UTF_8);
//    }
//
//
//    private static String n(String s) { return s == null ? "" : s; }
//    private static String q(String s) {
//        String v = n(s).replace("\"", "\"\"");
//        return "\"" + v + "\"";
//    }
//    private static String mask(String s) {
//        if (s == null || s.length() < 4) return "***";
//        int keep = Math.min(3, s.length());
//        return "***" + s.substring(s.length() - keep);
//    }

    public byte[] exportCsv(LocalDate date, ReservationStatus status, VisitorType visitorType, boolean maskContacts) {
        List<Reservation> list = reservations.findAllByVisitDate(date);
        if (status != null) {
            list = list.stream().filter(r -> r.getStatus() == status).collect(Collectors.toList());
        }
        if (visitorType != null) {
            list = list.stream().filter(r -> r.getVisitorType() == visitorType).collect(Collectors.toList());
        }

        final String SEP = ",";          // separador que queremos usar
        final String LS  = "\r\n";       // saltos Windows-friendly para Excel

        String header = String.join(SEP,
                "id","visit_date","first_name","last_name","dni","phone","email",
                "visitor_type","institution_name","institution_students",
                "adults_14_plus","minors","reduced_mobility","allergies",
                "origin_location","how_heard","status","created_at","updated_at");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');                 // BOM UTF-8 (acentos/ñ)
        sb.append("sep=").append(SEP).append(LS); // <-- pista para Excel
        sb.append(header).append(LS);

        for (Reservation r : list) {
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
                    q(String.valueOf(r.getAdults14Plus())),
                    q(String.valueOf(r.getMinors())),
                    q(String.valueOf(r.getReducedMobility())),
                    q(String.valueOf(r.isAllergies())),
                    q(n(r.getOriginLocation())),
                    q(r.getHowHeard() != null ? r.getHowHeard().name() : ""),
                    q(r.getStatus() != null ? r.getStatus().name() : ""),
                    q(r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""),
                    q(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "")
            )).append(LS);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String n(String s) { return s == null ? "" : s; }
    private static String q(String s) { String v = n(s).replace("\"","\"\""); return "\"" + v + "\""; }
    private static String mask(String s) {
        if (s == null || s.length() < 4) return "***";
        int keep = Math.min(3, s.length());
        return "***" + s.substring(s.length() - keep);
    }


    public Reservation findById(UUID id){
        return reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }
}


