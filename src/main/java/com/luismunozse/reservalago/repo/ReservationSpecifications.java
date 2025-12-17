package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ReservationSpecifications {

    private ReservationSpecifications() {
        // Utility class
    }

    public static Specification<Reservation> withDate(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.equal(root.get("visitDate"), date);
    }

    public static Specification<Reservation> withStatus(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> withDni(String dni) {
        return (root, query, cb) -> {
            if (dni == null || dni.isBlank()) {
                return null;
            }
            return cb.equal(root.get("dni"), dni);
        };
    }

    public static Specification<Reservation> withName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            String pattern = "%" + name.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    public static Specification<Reservation> withFilters(LocalDate date, ReservationStatus status, String dni, String name) {
        return Specification.allOf(
                withDate(date),
                withStatus(status),
                withDni(dni),
                withName(name)
        );
    }
}
