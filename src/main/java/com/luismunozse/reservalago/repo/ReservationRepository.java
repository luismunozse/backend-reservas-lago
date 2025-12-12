package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @Query("""
        select coalesce(sum(r.adults18Plus + r.children2To17 + r.babiesLessThan2), 0)
        from Reservation r
        where r.visitDate = :date and r.status <> 'CANCELLED'
        """)
    int totalPeopleForDate(@Param("date") LocalDate date);

    // NUEVOS: existencia por fecha + DNI (excluye canceladas)
    boolean existsByVisitDateAndDniAndStatusNot(
            LocalDate visitDate,
            String dni,
            ReservationStatus status);

    List<Reservation> findAllByDni(String dni);
    List<Reservation> findAllByVisitDateAndStatus(LocalDate date, ReservationStatus status);

    List<Reservation> findAllByVisitDate(LocalDate date);
    List<Reservation> findAllByStatus(ReservationStatus status);

    /**
     * Búsqueda optimizada con filtros dinámicos aplicados directamente en la base de datos.
     * Todos los parámetros son opcionales (null = no filtrar por ese criterio).
     *
     * @param date Fecha exacta de visita
     * @param monthStart Primer día del mes (para filtro por mes)
     * @param monthEnd Último día del mes (para filtro por mes)
     * @param yearStart Primer día del año (para filtro por año)
     * @param yearEnd Último día del año (para filtro por año)
     * @param status Estado de la reserva
     * @param visitorType Tipo de visitante
     * @param dni DNI del titular (ya normalizado)
     * @param name Nombre o apellido a buscar (case-insensitive)
     * @return Lista de reservas que cumplen TODOS los criterios especificados
     */
    @Query(value = """
        SELECT DISTINCT r.* FROM reservations r
        LEFT JOIN reservation_visitors v ON r.id = v.reservation_id
        WHERE (CAST(:date AS DATE) IS NULL OR r.visit_date = CAST(:date AS DATE))
          AND (CAST(:monthStart AS DATE) IS NULL OR r.visit_date >= CAST(:monthStart AS DATE))
          AND (CAST(:monthEnd AS DATE) IS NULL OR r.visit_date <= CAST(:monthEnd AS DATE))
          AND (CAST(:yearStart AS DATE) IS NULL OR r.visit_date >= CAST(:yearStart AS DATE))
          AND (CAST(:yearEnd AS DATE) IS NULL OR r.visit_date <= CAST(:yearEnd AS DATE))
          AND (CAST(:status AS VARCHAR) IS NULL OR r.status = CAST(:status AS VARCHAR))
          AND (CAST(:visitorType AS VARCHAR) IS NULL OR r.visitor_type = CAST(:visitorType AS VARCHAR))
          AND (CAST(:dni AS VARCHAR) IS NULL OR r.dni = CAST(:dni AS VARCHAR))
          AND (CAST(:name AS VARCHAR) IS NULL OR
               LOWER(r.first_name) LIKE LOWER('%' || CAST(:name AS VARCHAR) || '%') OR
               LOWER(r.last_name) LIKE LOWER('%' || CAST(:name AS VARCHAR) || '%') OR
               LOWER(v.first_name) LIKE LOWER('%' || CAST(:name AS VARCHAR) || '%') OR
               LOWER(v.last_name) LIKE LOWER('%' || CAST(:name AS VARCHAR) || '%'))
        ORDER BY r.visit_date ASC, r.created_at DESC
    """, nativeQuery = true)
    List<Reservation> findWithFilters(
        @Param("date") LocalDate date,
        @Param("monthStart") LocalDate monthStart,
        @Param("monthEnd") LocalDate monthEnd,
        @Param("yearStart") LocalDate yearStart,
        @Param("yearEnd") LocalDate yearEnd,
        @Param("status") String status,
        @Param("visitorType") String visitorType,
        @Param("dni") String dni,
        @Param("name") String name
    );
}

