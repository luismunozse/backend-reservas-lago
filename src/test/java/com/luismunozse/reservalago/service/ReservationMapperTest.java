package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.dto.VisitorDTO;
import com.luismunozse.reservalago.model.Circuit;
import com.luismunozse.reservalago.model.HowHeard;
import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.VisitorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMapperTest {

    private ReservationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReservationMapper();
    }

    @Test
    @DisplayName("normalizeDni debe remover puntos y guiones")
    void normalizeDni_removesDotsAndDashes() {
        assertThat(mapper.normalizeDni("12.345.678")).isEqualTo("12345678");
        assertThat(mapper.normalizeDni("12-345-678")).isEqualTo("12345678");
        assertThat(mapper.normalizeDni("12.345-678")).isEqualTo("12345678");
        assertThat(mapper.normalizeDni("12345678")).isEqualTo("12345678");
        assertThat(mapper.normalizeDni(null)).isNull();
    }

    @Test
    @DisplayName("fromCreateRequest debe mapear todos los campos correctamente")
    void fromCreateRequest_mapsAllFields() {
        var request = new CreateReservationRequest(
                LocalDate.of(2025, 1, 15),
                "Juan",
                "Perez",
                "12345678",
                "1155667788",
                "juan@test.com",
                null,
                Circuit.A,
                VisitorType.INDIVIDUAL,
                null,
                null,
                2,
                1,
                0,
                0,
                "Sin comentarios",
                "Buenos Aires",
                HowHeard.SOCIAL,
                true,
                List.of()
        );

        Reservation result = mapper.fromCreateRequest(request, "12345678");

        assertThat(result.getVisitDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(result.getFirstName()).isEqualTo("Juan");
        assertThat(result.getLastName()).isEqualTo("Perez");
        assertThat(result.getDni()).isEqualTo("12345678");
        assertThat(result.getPhone()).isEqualTo("1155667788");
        assertThat(result.getEmail()).isEqualTo("juan@test.com");
        assertThat(result.getCircuit()).isEqualTo(Circuit.A);
        assertThat(result.getVisitorType()).isEqualTo(VisitorType.INDIVIDUAL);
        assertThat(result.getAdults18Plus()).isEqualTo(2);
        assertThat(result.getChildren2To17()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("fromCreateRequest debe mapear visitantes con telefono opcional")
    void fromCreateRequest_mapsVisitorsWithOptionalPhone() {
        var visitors = List.of(
                new VisitorDTO("Maria", "Garcia", "87654321", "1199887766"),
                new VisitorDTO("Pedro", "Lopez", "11223344", null)
        );

        var request = new CreateReservationRequest(
                LocalDate.of(2025, 1, 15),
                "Juan",
                "Perez",
                "12345678",
                "1155667788",
                "juan@test.com",
                null,
                Circuit.A,
                VisitorType.INDIVIDUAL,
                null,
                null,
                2,
                0,
                0,
                0,
                null,
                "Buenos Aires",
                HowHeard.SOCIAL,
                true,
                visitors
        );

        Reservation result = mapper.fromCreateRequest(request, "12345678");

        assertThat(result.getVisitors()).hasSize(2);

        var visitor1 = result.getVisitors().get(0);
        assertThat(visitor1.getFirstName()).isEqualTo("Maria");
        assertThat(visitor1.getLastName()).isEqualTo("Garcia");
        assertThat(visitor1.getDni()).isEqualTo("87654321");
        assertThat(visitor1.getPhone()).isEqualTo("1199887766");

        var visitor2 = result.getVisitors().get(1);
        assertThat(visitor2.getFirstName()).isEqualTo("Pedro");
        assertThat(visitor2.getPhone()).isNull();
    }
}
