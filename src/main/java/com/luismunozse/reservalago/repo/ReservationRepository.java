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
        select coalesce(sum(r.adults14Plus + r.minors), 0)
        from Reservation r
        where r.visitDate = :date and r.status <> 'CANCELLED'
        """)
    int totalPeopleForDate(@Param("date") LocalDate date);

    List<Reservation> findAllByVisitDateAndStatus(LocalDate date, ReservationStatus status);

    List<Reservation> findAllByVisitDate(LocalDate date);
}
