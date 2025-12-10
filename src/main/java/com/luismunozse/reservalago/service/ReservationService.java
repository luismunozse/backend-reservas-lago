package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.*;
import com.luismunozse.reservalago.model.*;
import com.luismunozse.reservalago.repo.ReservationRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservations;
    private final SystemConfigService systemConfigService;
    private final ReservationMapper reservationMapper;
    private final ReservationExcelExporter reservationExcelExporter;
    private final AvailabilityService availabilityService;
    private final WhatsAppService whatsAppService;

    @Transactional
    public UUID create(CreateReservationRequest req) {
        log.info("Creando reserva: fecha={}, dni={}, tipo={}, pax={}",
                req.visitDate(), req.dni(), req.visitorType(),
                req.adults18Plus() + req.children2To17() + req.babiesLessThan2());

        String dni = reservationMapper.normalizeDni(req.dni());

        int capacity = availabilityService.capacityFor(req.visitDate());
        // int used = reservations.totalPeopleForDate(req.visitDate());
        // int requested = req.adults18Plus() + req.children2To17() + req.babiesLessThan2();
        // if (used + requested > capacity) {
        //     throw new IllegalStateException("No hay cupo para esa fecha");
        // }
        // if (req.visitorType() == VisitorType.EDUCATIONAL_INSTITUTION) {
        //     // Verificar si las reservas para instituciones educativas están habilitadas
        //     if (!systemConfigService.isEducationalReservationsEnabled()) {
        //         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
        //             "Las reservas para instituciones educativas no están habilitadas en este momento");
        //     }
        //     if (req.institutionName() == null || req.institutionName().isBlank()) {
        //         throw new IllegalArgumentException("Para instituciones, 'institutionName' es obligatorio");
        //     }
        // }

        int used = reservations.totalPeopleForDate(req.visitDate());
        int requested = req.adults18Plus() + req.children2To17() + req.babiesLessThan2();

        //Debe haber al menos una persona
        if (requested <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La reserva debe incluir al menos una persona");
        }

        //Sin cupo → error consistente
        if (used + requested > capacity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay cupo disponible para esa fecha");
        }

        //Reglas extra para instituciones educativas
        if (req.visitorType() == VisitorType.EDUCATIONAL_INSTITUTION) {
            if (!systemConfigService.isEducationalReservationsEnabled()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Las reservas para instituciones educativas no están habilitadas en este momento");
            }
            if (req.institutionName() == null || req.institutionName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para instituciones, 'institutionName' es obligatorio");
            }
            if (req.institutionStudents() == null || req.institutionStudents() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para instituciones, 'institutionStudents' debe ser mayor a 0");
            }
        }


        // ✅ Pre-chequeo de duplicado (fecha + DNI, excluyendo canceladas)
        if (dni != null && reservations.existsByVisitDateAndDniAndStatusNot(
                req.visitDate(), dni, ReservationStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una visita con ese DNI en esa fecha.");
        }

        Reservation r = reservationMapper.fromCreateRequest(req, dni);

        try {
            reservations.save(r);
            log.info("Reserva creada exitosamente: id={}, fecha={}, dni={}",
                    r.getId(), r.getVisitDate(), dni);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Error de integridad al crear reserva: dni={}, fecha={}, error={}",
                    dni, req.visitDate(), ex.getMessage());
            handleDataIntegrityViolation(ex);
        }

        return r.getId();
    }

    @Transactional
    public UUID createEvent(CreateEventRequest req) {
        log.info("Creando evento: titulo={}, fecha={}, cupo={}",
                req.titulo(), req.fechaISO(), req.cupo());
        Reservation r = reservationMapper.fromCreateEventRequest(req);
        reservations.save(r);
        log.info("Evento creado: id={}", r.getId());
        return r.getId();
    }



    @Transactional(readOnly = true)
    public byte[] exportExcel(LocalDate date, java.time.YearMonth month, Integer year,
                              ReservationStatus status, VisitorType visitorType,
                              String dni, String name, boolean maskContacts) {
        String normalizedDni = reservationMapper.normalizeDni(dni);

        // Calcular rangos para mes y año
        LocalDate monthStart = null;
        LocalDate monthEnd = null;
        if (month != null) {
            monthStart = month.atDay(1);
            monthEnd = month.atEndOfMonth();
        }

        LocalDate yearStart = null;
        LocalDate yearEnd = null;
        if (year != null) {
            yearStart = LocalDate.of(year, 1, 1);
            yearEnd = LocalDate.of(year, 12, 31);
        }

        // Consulta optimizada en base de datos (native query requiere Strings para enums)
        List<Reservation> list = reservations.findWithFilters(
            date,
            monthStart,
            monthEnd,
            yearStart,
            yearEnd,
            status != null ? status.name() : null,
            visitorType != null ? visitorType.name() : null,
            normalizedDni,
            name
        );

        // Límite de seguridad para evitar exportaciones masivas
        if (list.size() > 10000) {
            log.warn("Exportación rechazada: demasiados registros ({})", list.size());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Demasiados registros (" + list.size() + "). Límite: 10,000. Aplique más filtros.");
        }

        log.info("Exportando {} reservas a Excel", list.size());
        return reservationExcelExporter.exportExcel(list, maskContacts);
    }


    public Reservation findById(UUID id){
        return reservations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<AdminReservationDTO> adminList(LocalDate date, ReservationStatus status, String dni) {
        List<Reservation> list;
        String normalizedDni = reservationMapper.normalizeDni(dni);

        if (normalizedDni != null && !normalizedDni.isBlank()) {
            list = reservations.findAllByDni(normalizedDni);
            if (date != null) {
                list = list.stream()
                        .filter(r -> date.equals(r.getVisitDate()))
                        .toList();
            }
            if (status != null) {
                list = list.stream()
                        .filter(r -> status == r.getStatus())
                        .toList();
            }
        } else if (date != null && status != null) {
            list = reservations.findAllByVisitDateAndStatus(date, status);
        } else if (date != null) {
            list = reservations.findAllByVisitDate(date);
        } else if (status != null) {
            list = reservations.findAllByStatus(status);
        } else {
            list = reservations.findAll();
        }

        return list.stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(reservationMapper::toAdminDTO)
                .toList();
    }


    @Transactional
    public void confirmReservation(UUID id) {
        log.info("Confirmando reserva: id={}", id);
        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> {
                    log.warn("Reserva no encontrada para confirmar: id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada");
                });
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservations.save(reservation);
        log.info("Reserva confirmada: id={}, dni={}, fecha={}",
                id, reservation.getDni(), reservation.getVisitDate());

        // Enviar notificación por WhatsApp (asíncrono)
        whatsAppService.sendConfirmation(reservation);
    }

    @Transactional
    public void cancelReservation(UUID id) {
        log.info("Cancelando reserva: id={}", id);
        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> {
                    log.warn("Reserva no encontrada para cancelar: id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada");
                });
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservations.save(reservation);
        log.info("Reserva cancelada: id={}, dni={}, fecha={}",
                id, reservation.getDni(), reservation.getVisitDate());
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String root = ex.getMessage() != null ? ex.getMessage() : "";
        if (root.contains("ux_reservations_date_dni")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una visita con ese DNI en esa fecha.");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos", ex);
    }


}


