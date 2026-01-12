package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.AdminReservationDTO;
import com.luismunozse.reservalago.dto.AdminVisitorDTO;
import com.luismunozse.reservalago.dto.CreateEventRequest;
import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.dto.VisitorDTO;
import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.ReservationVisitor;
import com.luismunozse.reservalago.model.VisitorType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class ReservationMapper {

    public String normalizeDni(String dni) {
        if (dni == null) {
            return null;
        }
        return dni.trim().replace(".", "").replace("-", "");
    }

    private String normalizePlate(String plate) {
        if (plate == null || plate.isBlank()) return null;
        return plate.trim().toUpperCase();
    }


    public Reservation fromCreateRequest(CreateReservationRequest req, String normalizedDni) {
        Reservation r = new Reservation();
        r.setVisitDate(req.visitDate());
        r.setFirstName(req.firstName());
        r.setLastName(req.lastName());
        r.setDni(normalizedDni);
        r.setPhone(req.phone());
        r.setEmail(req.email());
        r.setVehiclePlate(normalizePlate(req.vehiclePlate()));
        r.setCircuit(req.circuit());
        r.setVisitorType(req.visitorType());
        r.setInstitutionName(req.institutionName());
        r.setInstitutionStudents(req.institutionStudents());
        r.setAdults18Plus(req.adults18Plus());
        r.setChildren2To17(req.children2To17());
        r.setBabiesLessThan2(req.babiesLessThan2());
        r.setReducedMobility(req.reducedMobility());
        r.setComment(req.comment());
        r.setOriginLocation(req.originLocation());
        r.setHowHeard(req.howHeard());
        r.setAcceptedPolicies(req.acceptedPolicies());
        r.setStatus(ReservationStatus.PENDING);

        if (req.visitors() != null && !req.visitors().isEmpty()) {
            for (VisitorDTO v : req.visitors()) {
                ReservationVisitor rv = new ReservationVisitor();
                rv.setReservation(r);
                rv.setFirstName(v.firstName());
                rv.setLastName(v.lastName());
                rv.setDni(normalizeDni(v.dni()));
                rv.setPhone(v.phone());
                r.getVisitors().add(rv);
            }
        }

        return r;
    }

    public Reservation fromCreateEventRequest(CreateEventRequest req) {
        LocalDate visitDate = req.fechaISO().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        Reservation r = new Reservation();
        r.setVisitDate(visitDate);
        r.setVisitorType(VisitorType.EVENT);

        r.setFirstName(req.titulo() != null ? req.titulo() : "Evento");
        r.setLastName("Evento");

        r.setDni("00000000");
        r.setPhone("0000000000");
        r.setEmail("evento@reservalago.com");

        if (req.circuito() != null && !req.circuito().isBlank()) {
            try {
                r.setCircuit(com.luismunozse.reservalago.model.Circuit.valueOf(req.circuito()));
            } catch (IllegalArgumentException e) {
                r.setCircuit(com.luismunozse.reservalago.model.Circuit.A);
            }
        } else {
            r.setCircuit(com.luismunozse.reservalago.model.Circuit.A);
        }

        int cupo = req.cupo() != null ? req.cupo() : 0;
        r.setAdults18Plus(cupo);
        r.setChildren2To17(0);
        r.setBabiesLessThan2(0);

        r.setComment(req.notas());

        r.setReducedMobility(0);
        r.setOriginLocation("N/A");
        r.setHowHeard(com.luismunozse.reservalago.model.HowHeard.OTHER);
        r.setAcceptedPolicies(true);
        r.setStatus(ReservationStatus.CONFIRMED);

        return r;
    }

    public AdminReservationDTO toAdminDTO(Reservation r) {
        List<AdminVisitorDTO> visitors = r.getVisitors() == null
                ? List.of()
                : r.getVisitors().stream()
                .sorted(Comparator.comparing(ReservationVisitor::getFirstName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(v -> new AdminVisitorDTO(
                        v.getFirstName(),
                        v.getLastName(),
                        v.getDni(),
                        v.getPhone()
                ))
                .toList();

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
                r.getVehiclePlate(),
                r.getCircuit() != null ? r.getCircuit().name() : null,
                r.getVisitorType() != null ? r.getVisitorType().name() : null,
                r.getOriginLocation(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCreatedAt(),
                r.getDni(),
                r.getReducedMobility(),
                r.getComment(),
                visitors
        );
    }
}

