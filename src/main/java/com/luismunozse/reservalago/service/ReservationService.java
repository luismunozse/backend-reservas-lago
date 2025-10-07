package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.AdminReservationDTO;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservations;
    private final AvailabilityRuleRepository availability;
    private final EmailService emailService;

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
        int requested = req.adults18Plus() + req.children2To17() + req.babiesLessThan2();
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
        r.setAdults18Plus(req.adults18Plus());
        r.setChildren2To17(req.children2To17());
        r.setBabiesLessThan2(req.babiesLessThan2());
        r.setReducedMobility(req.reducedMobility());
        r.setAllergies(req.allergies());
        r.setComment(req.comment());
        r.setOriginLocation(req.originLocation());
        r.setHowHeard(req.howHeard());
        r.setAcceptedPolicies(req.acceptedPolicies());
        r.setStatus(ReservationStatus.PENDING);

        reservations.save(r);

        // Enviar email de confirmación
        emailService.sendReservationConfirmation(r);
        return r.getId();
    }

    @Scheduled(cron = "0 0 * * * *") // cada hora
    public void sendReminders() {
        LocalDate in48h = LocalDate.now().plusDays(2);
        List<Reservation> list = reservations.findAllByVisitDateAndStatus(in48h, ReservationStatus.PENDING);
        for (Reservation r : list) {
            // TODO: Crear template específico para recordatorios
            emailService.sendReservationConfirmation(r);
        }
    }


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
                "adults_18_plus","children_2_to_17","babies_less_than_2","reduced_mobility","allergies",
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

    public List<AdminReservationDTO> adminList(LocalDate date, ReservationStatus status) {
        List<com.luismunozse.reservalago.model.Reservation> list;

        if (date != null && status != null) {
            list = reservations.findAllByVisitDateAndStatus(date, status);
        } else if (date != null) {
            list = reservations.findAllByVisitDate(date);
        } else if (status != null) {
            list = reservations.findAllByStatus(status);
        } else {
            list = reservations.findAll();
        }

        // Orden null-safe por createdAt desc
        return list.stream()
                .sorted(Comparator.comparing(
                        com.luismunozse.reservalago.model.Reservation::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .map(this::toAdminDTO)
                .toList();
    }


    @Transactional
    public void confirmReservation(UUID id) {
        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservations.save(reservation);
        
        // Enviar email de confirmación actualizada
        emailService.sendReservationConfirmation(reservation);
    }

    @Transactional
    public void cancelReservation(UUID id) {
        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservations.save(reservation);
        
        // Enviar email de cancelación
        emailService.sendReservationCancellation(reservation);
    }

    public List<Map<String, Object>> availabilityForMonth(LocalDate month) {
        LocalDate firstDay = month.withDayOfMonth(1);
        LocalDate lastDay = month.withDayOfMonth(month.lengthOfMonth());
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            Map<String, Object> dayAvailability = availabilityFor(date);
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("availableDate", date.toString());
            formatted.put("totalCapacity", dayAvailability.get("capacity"));
            formatted.put("remainingCapacity", dayAvailability.get("remaining"));
            result.add(formatted);
        }
        
        return result;
    }

    private AdminReservationDTO toAdminDTO(Reservation r) {
        return new AdminReservationDTO(
                r.getId(),
                r.getVisitDate(),
                r.getFirstName(),
                r.getLastName(),
                r.getAdults18Plus(),
                r.getChildren2To17(),
                r.getBabiesLessThan2(),
                r.getEmail(),
                r.getPhone(),
                r.getCircuit() != null ? r.getCircuit().name() : null,
                r.getVisitorType() != null ? r.getVisitorType().name() : null,
                r.getOriginLocation(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCreatedAt() 
        );
    }

}


