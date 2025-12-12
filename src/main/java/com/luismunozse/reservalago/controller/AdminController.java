package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.AdminReservationDTO;
import com.luismunozse.reservalago.dto.CreateEventRequest;
import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.VisitorType;
import com.luismunozse.reservalago.repo.AvailabilityRuleRepository;
import com.luismunozse.reservalago.service.ReservationService;
import com.luismunozse.reservalago.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AvailabilityRuleRepository availability;
    private final ReservationService reservationService;
    private final SystemConfigService systemConfigService;

    @Operation(summary = "Upsert de capacidad por día",
            description = "Crea o actualiza la capacidad máxima de visitantes para una fecha específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Capacidad actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "capacity no puede ser negativa" }
                                    """)))
    })
    @PutMapping("/availability/{date}")
    public void upsert(@PathVariable LocalDate date, @RequestBody Map<String, Integer> body) {
        int capacity = body.getOrDefault("capacity", 0);

        // Fix #6: Validar que la capacidad no sea negativa
        if (capacity < 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "La capacidad no puede ser negativa");
        }

        log.info("Actualizando capacidad: fecha={}, capacidad={}", date, capacity);
        AvailabilityRule rule = availability.findByDay(date).orElseGet(AvailabilityRule::new);
        rule.setDay(date);
        rule.setCapacity(capacity);
        availability.save(rule);
    }

    // Exportación Excel
    @Operation(summary = "Exportar reservas Excel",
            description = "Exporta las reservas en formato Excel (XLSX), con filtros opcionales por fecha, mes, año, estado, tipo de visitante, DNI y nombre. Los datos de contacto se exportan completos. Límite máximo: 10,000 registros.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo Excel generado correctamente",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "400", description = "Demasiados registros. Aplique más filtros"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para exportar reservas")
    })
    @GetMapping("/reservations/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String month, // formato YYYY-MM
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) VisitorType visitorType,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String name // Nombre o apellido a buscar
    ) {
        java.time.YearMonth ym = null;
        if (month != null && !month.isBlank()) {
            ym = java.time.YearMonth.parse(month);
        }

        // Para uso administrativo exportamos siempre datos completos (sin enmascarar)
        byte[] data = reservationService.exportExcel(date, ym, year, status, visitorType, dni, name, false);

        String filename;
        if (date != null) {
            filename = String.format("reservas_%s.xlsx", date);
        } else if (ym != null) {
            filename = String.format("reservas_%s.xlsx", ym);
        } else if (year != null) {
            filename = String.format("reservas_%s.xlsx", year);
        } else {
            filename = "reservas.xlsx";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @Operation(summary = "Listar reservas (filtros opcionales: date, status)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de reservas",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminReservationDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para ver reservas")
    })
    @GetMapping({"/reservations", "/reservations/"})
    public java.util.List<AdminReservationDTO> listReservations(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String dni
    ) {
        return reservationService.adminList(date, status, dni);
    }

    @Operation(summary = "Confirmar una reserva",
            description = "Marca una reserva existente como CONFIRMED y envía el email de confirmación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva confirmada correctamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Reserva no encontrada" }
                                    """)))
    })
    @PostMapping("/reservations/{id}/confirm")
    public void confirmReservation(@PathVariable java.util.UUID id) {
        log.info("Admin: confirmando reserva id={}", id);
        reservationService.confirmReservation(id);
    }

    @Operation(summary = "Cancelar una reserva",
            description = "Marca una reserva existente como CANCELLED y envía el email de cancelación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva cancelada correctamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Reserva no encontrada" }
                                    """)))
    })
    @PostMapping("/reservations/{id}/cancel")
    public void cancelReservation(@PathVariable java.util.UUID id) {
        log.info("Admin: cancelando reserva id={}", id);
        reservationService.cancelReservation(id);
    }

    @Operation(summary = "Crear un evento (reserva tipo EVENT)",
            description = "Crea una reserva especial de tipo EVENT con capacidad (cupo) predefinida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento creado correctamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "id": "c1a2b3c4-d5e6-7890-ab12-34567890cdef" }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Datos inválidos" }
                                    """)))
    })
    @PostMapping("/eventos")
    public Map<String, String> createEvent(@RequestBody CreateEventRequest req) {
        java.util.UUID id = reservationService.createEvent(req);
        return Map.of("id", id.toString());
    }

    @Operation(summary = "Obtener estado de reservas para instituciones educativas")
    @GetMapping("/config/educational-reservations")
    public Map<String, Boolean> getEducationalReservationsStatus() {
        boolean enabled = systemConfigService.isEducationalReservationsEnabled();
        return Map.of("enabled", enabled);
    }

    @Operation(summary = "Habilitar/deshabilitar reservas para instituciones educativas")
    @PutMapping("/config/educational-reservations")
    public Map<String, Boolean> toggleEducationalReservations(@RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        log.info("Configuración: reservas educativas enabled={}", enabled);
        systemConfigService.setEducationalReservationsEnabled(enabled);
        return Map.of("enabled", enabled);
    }
}
