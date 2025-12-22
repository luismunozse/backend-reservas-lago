package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.ReservationVisitor;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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

    /**
     * Busca por DNI con coincidencia parcial (LIKE) en titular y visitantes.
     * Permite buscar apenas se va ingresando el DNI, sin esperar los 8 dígitos completos.
     */
    public static Specification<Reservation> withDni(String dni) {
        return (root, query, cb) -> {
            if (dni == null || dni.isBlank()) {
                return null;
            }
            // Coincidencia parcial: busca DNIs que contengan el texto ingresado
            String pattern = "%" + dni + "%";

            // Buscar en el DNI del titular
            Predicate titularDni = cb.like(root.get("dni"), pattern);

            // Buscar en los DNIs de los visitantes/acompañantes
            Join<Reservation, ReservationVisitor> visitors = root.join("visitors", JoinType.LEFT);
            Predicate visitorDni = cb.like(visitors.get("dni"), pattern);

            // Evitar duplicados en los resultados
            if (query != null) {
                query.distinct(true);
            }

            return cb.or(titularDni, visitorDni);
        };
    }

    /**
     * Busca por nombre o apellido en titular y visitantes/acompañantes.
     */
    public static Specification<Reservation> withName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            String pattern = "%" + name.toLowerCase() + "%";

            // Buscar en nombre/apellido del titular
            Predicate titularFirstName = cb.like(cb.lower(root.get("firstName")), pattern);
            Predicate titularLastName = cb.like(cb.lower(root.get("lastName")), pattern);

            // Buscar en nombre/apellido de los visitantes/acompañantes
            Join<Reservation, ReservationVisitor> visitors = root.join("visitors", JoinType.LEFT);
            Predicate visitorFirstName = cb.like(cb.lower(visitors.get("firstName")), pattern);
            Predicate visitorLastName = cb.like(cb.lower(visitors.get("lastName")), pattern);

            // Evitar duplicados en los resultados
            if (query != null) {
                query.distinct(true);
            }

            return cb.or(titularFirstName, titularLastName, visitorFirstName, visitorLastName);
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
