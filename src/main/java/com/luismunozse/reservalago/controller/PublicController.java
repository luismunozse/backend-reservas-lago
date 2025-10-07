package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.dto.ReservationSummaryDTO;
import com.luismunozse.reservalago.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Público")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final ReservationService service;

    @Operation(summary = "Disponibilidad por dia")
    @GetMapping("/availability")
    public Object availability(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String month) {
        
        if (month != null && !month.isEmpty()) {
            // Convertir formato YYYY-MM a LocalDate (primer día del mes)
            LocalDate monthDate = LocalDate.parse(month + "-01");
            return service.availabilityForMonth(monthDate);
        } else if (date != null) {
            return service.availabilityFor(date);
        } else {
            throw new IllegalArgumentException("Debe proporcionar 'date' o 'month'");
        }
    }

    @Operation(summary = "Crear una reserva")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = CreateReservationRequest.class))
    )
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody CreateReservationRequest req) {
        UUID id = service.create(req);
        return Map.of("id", id.toString(), "status", "PENDING");
    }

    @Operation(summary = "Obtener resumen de una reserva por ID")
    @GetMapping("/reservations/{id}")
    public ReservationSummaryDTO get(@PathVariable UUID id){
        return ReservationSummaryDTO.from(service.findById(id));
    }

}
