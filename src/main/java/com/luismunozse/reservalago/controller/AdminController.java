package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.AdminReservationDTO;
import com.luismunozse.reservalago.dto.CapacityRequest;
import com.luismunozse.reservalago.dto.CreateEventRequest;
import com.luismunozse.reservalago.dto.EducationalReservationsRequest;
import com.luismunozse.reservalago.dto.ExportReservationsFilter;
import jakarta.validation.Valid;
import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.model.ReservationStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public void upsert(@PathVariable LocalDate date, @Valid @RequestBody CapacityRequest request) {
        log.info("Actualizando capacidad: fecha={}, capacidad={}", date, request.getCapacity());
        AvailabilityRule rule = availability.findByDay(date).orElseGet(AvailabilityRule::new);
        rule.setDay(date);
        rule.setCapacity(request.getCapacity());
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
    public ResponseEntity<byte[]> export(@ModelAttribute ExportReservationsFilter filter) {
        java.time.YearMonth ym = null;
        if (filter.getMonth() != null && !filter.getMonth().isBlank()) {
            ym = java.time.YearMonth.parse(filter.getMonth());
        }

        // Para uso administrativo exportamos siempre datos completos (sin enmascarar)
        byte[] data = reservationService.exportExcel(
                filter.getDate(), ym, filter.getYear(), filter.getStatus(),
                filter.getVisitorType(), filter.getDni(), filter.getName(), false);

        String filename = buildExportFilename(filter.getDate(), ym, filter.getYear());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private String buildExportFilename(LocalDate date, java.time.YearMonth ym, Integer year) {
        if (date != null) {
            return String.format("reservas_%s.xlsx", date);
        } else if (ym != null) {
            return String.format("reservas_%s.xlsx", ym);
        } else if (year != null) {
            return String.format("reservas_%s.xlsx", year);
        }
        return "reservas.xlsx";
    }

    @Operation(summary = "Listar reservas paginadas",
            description = "Lista reservas con paginación y filtros. Parámetros: page (0-indexed), size (default 20), sort (ej: createdAt,desc)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de reservas",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminReservationDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para ver reservas")
    })
    @GetMapping({"/reservations", "/reservations/"})
    public Page<AdminReservationDTO> listReservations(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reservationService.adminListPaged(date, status, dni, name, pageable);
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

    @Operation(summary = "Eliminar una reserva",
            description = "Elimina permanentemente una reserva de la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reserva eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Reserva no encontrada" }
                                    """)))
    })
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable java.util.UUID id) {
        log.info("Admin: eliminando reserva id={}", id);
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
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
    public Map<String, Boolean> toggleEducationalReservations(@Valid @RequestBody EducationalReservationsRequest request) {
        log.info("Configuración: reservas educativas enabled={}", request.getEnabled());
        systemConfigService.setEducationalReservationsEnabled(request.getEnabled());
        return Map.of("enabled", request.getEnabled());
    }

    @GetMapping("config/default-capacity")
    public Map<String, Integer> getDefaultCapacity() {
        return Map.of("capacity", systemConfigService.getDefaultCapacity());
    }

    @PutMapping("config/default-capacity")
    public void setDefaultCapacity(@RequestBody Map<String, Integer> body) {
        Integer cap = body.get("capacity");
        systemConfigService.setDefaultCapacity(cap);
    }
}
