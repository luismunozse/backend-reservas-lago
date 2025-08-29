package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.dto.ReservationSummaryDTO;
import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final ReservationService service;

    @GetMapping("/availability")
    public Map<String, Object> availability(@RequestParam LocalDate date) {
        return service.availabilityFor(date);
    }


    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody CreateReservationRequest req) {
        UUID id = service.create(req);
        return Map.of("id", id.toString(), "status", "PENDING");
    }

    @GetMapping("/reservations/{id}")
    public ReservationSummaryDTO get(@PathVariable UUID id){
        return ReservationSummaryDTO.from(service.findById(id));
    }

}
