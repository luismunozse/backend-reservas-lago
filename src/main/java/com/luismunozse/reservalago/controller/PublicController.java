package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.dto.ReservationSummaryDTO;
import com.luismunozse.reservalago.service.ReservationService;
import com.luismunozse.reservalago.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;

    @Operation(
            summary = "Disponibilidad",
            description = "Consulta disponibilidad por día o por mes. Enviar exactamente uno de los parámetros: 'date' (YYYY-MM-DD) o 'month' (YYYY-MM).",
            tags = {"Disponibilidad"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad retornada",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Por día", value = "{\n  \"date\": \"2025-09-02\",\n  \"capacity\": 30,\n  \"remaining\": 12\n}"),
                                    @ExampleObject(name = "Por mes", value = "[\n  {\n    \"availableDate\": \"2025-09-01\",\n    \"totalCapacity\": 30,\n    \"remainingCapacity\": 20\n  },\n  {\n    \"availableDate\": \"2025-09-02\",\n    \"totalCapacity\": 30,\n    \"remainingCapacity\": 12\n  }\n]")
                            })) ,
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"El parámetro 'month' debe tener formato 'YYYY-MM' (ej: 2025-09)\"\n}")))
    })
    @GetMapping("/availability")
    public Object availability(
            @Parameter(description = "Fecha específica (YYYY-MM-DD)")
            @RequestParam(required = false) LocalDate date,
            @Parameter(description = "Mes específico (YYYY-MM)")
            @RequestParam(required = false) String month) {

        // No permitir enviar ambos parámetros a la vez
        if (date != null && month != null && !month.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No combine 'date' y 'month' en la misma solicitud");
        }

        if (month != null && !month.isBlank()) {
            // Convertir formato YYYY-MM a LocalDate (primer día del mes)
            try {
                YearMonth ym = YearMonth.parse(month); // ISO 'yyyy-MM'
                LocalDate monthDate = ym.atDay(1);
                return availabilityService.availabilityForMonth(monthDate);
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El parámetro 'month' debe tener formato 'YYYY-MM' (ej: 2025-09)");
            }
        } else if (date != null) {
            return availabilityService.availabilityFor(date);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe proporcionar exactamente uno de 'date' o 'month'");
        }
    }

    @Operation(summary = "Crear una reserva", tags = {"Reservas"})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = CreateReservationRequest.class))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva creada",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"id\": \"c1a2b3c4-d5e6-7890-ab12-34567890cdef\",\n  \"status\": \"PENDING\"\n}"))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"dni: no debe estar en blanco\"\n}"))),
            @ApiResponse(responseCode = "403", description = "Reservas educativas deshabilitadas",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"Las reservas para instituciones educativas no están habilitadas en este momento\"\n}"))),
            @ApiResponse(responseCode = "409", description = "Reserva duplicada (mismo día y DNI)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"Ya existe una reserva para ese DNI en esa fecha.\"\n}")))
    })
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody CreateReservationRequest req) {
        log.info("Nueva reserva recibida: fecha={}, tipo={}, email={}",
                req.visitDate(), req.visitorType(), req.email());
        UUID id = reservationService.create(req);
        return Map.of("id", id.toString(), "status", "PENDING");
    }

    @Operation(summary = "Obtener resumen de una reserva por ID", tags = {"Reservas"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen de reserva",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReservationSummaryDTO.class))),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"Reserva no encontrada\"\n}")))
    })
    @GetMapping("/reservations/{id}")
    public ReservationSummaryDTO get(@PathVariable UUID id){
        return ReservationSummaryDTO.from(reservationService.findById(id));
    }

}
