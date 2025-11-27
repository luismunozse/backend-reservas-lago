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
}

