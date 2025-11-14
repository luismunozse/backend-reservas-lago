package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.repo.AvailabilityRuleRepository;
import com.luismunozse.reservalago.repo.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRuleRepository availability;
    private final ReservationRepository reservations;

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

    public int capacityFor(LocalDate day) {
        return (int) availabilityFor(day).get("capacity");
    }
}

