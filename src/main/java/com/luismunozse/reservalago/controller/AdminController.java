package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.VisitorType;
import com.luismunozse.reservalago.repo.AvailabilityRuleRepository;
import com.luismunozse.reservalago.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AvailabilityRuleRepository availability;
    private final ReservationService reservationService;

    @PutMapping("/availability/{date}")
    public void upsert(@PathVariable LocalDate date, @RequestBody Map<String, Integer> body) {
        int capacity = body.getOrDefault("capacity", 0);
        AvailabilityRule rule = availability.findByDay(date).orElseGet(AvailabilityRule::new);
        rule.setDay(date);
        rule.setCapacity(capacity);
        availability.save(rule);
    }

    // Exportación CSV
    @GetMapping("/reservations/export")
    public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) VisitorType visitorType,
            @RequestParam(required = false, defaultValue = "false") boolean mask
    ) {
        byte[] data = reservationService.exportCsv(date, status, visitorType, mask);
        String filename = String.format("reservas_%s.csv", date);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }
}
