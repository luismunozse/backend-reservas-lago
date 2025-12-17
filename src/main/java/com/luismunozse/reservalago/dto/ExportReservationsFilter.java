package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.VisitorType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ExportReservationsFilter {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String month; // formato YYYY-MM

    private Integer year;

    private ReservationStatus status;

    private VisitorType visitorType;

    private String dni;

    private String name; // Nombre o apellido a buscar
}
