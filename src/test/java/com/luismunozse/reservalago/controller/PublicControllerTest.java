package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.model.*;
import com.luismunozse.reservalago.service.AvailabilityService;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
@Import(TestSecurityConfig.class)
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    @DisplayName("GET /api/availability")
    class AvailabilityEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Debe retornar disponibilidad por fecha")
        void shouldReturnAvailabilityByDate() throws Exception {
            LocalDate date = LocalDate.of(2025, 9, 15);
            Map<String, Object> availability = Map.of(
                    "date", date,
                    "capacity", 30,
                    "remaining", 25
            );

            when(availabilityService.availabilityFor(date)).thenReturn(availability);

            mockMvc.perform(get("/api/availability")
                            .param("date", "2025-09-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.capacity").value(30))
                    .andExpect(jsonPath("$.remaining").value(25));
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar disponibilidad por mes")
        void shouldReturnAvailabilityByMonth() throws Exception {
            List<Map<String, Object>> list = List.of(
                    Map.of("availableDate", "2025-09-01", "totalCapacity", 30, "remainingCapacity", 20),
                    Map.of("availableDate", "2025-09-02", "totalCapacity", 30, "remainingCapacity", 15)
            );

            when(availabilityService.availabilityForMonth(any())).thenReturn(list);

            mockMvc.perform(get("/api/availability")
                            .param("month", "2025-09"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @WithMockUser
        @DisplayName("Debe rechazar si se envían ambos parámetros")
        void shouldRejectBothParams() throws Exception {
            mockMvc.perform(get("/api/availability")
                            .param("date", "2025-09-15")
                            .param("month", "2025-09"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe rechazar si no se envía ningún parámetro")
        void shouldRejectNoParams() throws Exception {
            mockMvc.perform(get("/api/availability"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe rechazar formato de mes inválido")
        void shouldRejectInvalidMonthFormat() throws Exception {
            mockMvc.perform(get("/api/availability")
                            .param("month", "09-2025"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/reservations")
    class CreateReservationEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Debe crear reserva exitosamente")
        void shouldCreateReservation() throws Exception {
            UUID expectedId = UUID.randomUUID();
            when(reservationService.create(any())).thenReturn(expectedId);

            var request = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Juan", "Perez", "12345678", "1155667788", "juan@test.com",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    2, 1, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, true, List.of()
            );

            mockMvc.perform(post("/api/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(expectedId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("Debe rechazar DNI con formato inválido")
        void shouldRejectInvalidDni() throws Exception {
            var request = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Juan", "Perez", "123", "1155667788", "juan@test.com",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    2, 1, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, true, List.of()
            );

            mockMvc.perform(post("/api/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe rechazar email inválido")
        void shouldRejectInvalidEmail() throws Exception {
            var request = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Juan", "Perez", "12345678", "1155667788", "invalid-email",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    2, 1, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, true, List.of()
            );

            mockMvc.perform(post("/api/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar 409 cuando hay reserva duplicada")
        void shouldReturn409WhenDuplicate() throws Exception {
            when(reservationService.create(any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una visita"));

            var request = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Juan", "Perez", "12345678", "1155667788", "juan@test.com",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    2, 1, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, true, List.of()
            );

            mockMvc.perform(post("/api/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser
        @DisplayName("Debe rechazar si acceptedPolicies es false")
        void shouldRejectIfPoliciesNotAccepted() throws Exception {
            var request = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Juan", "Perez", "12345678", "1155667788", "juan@test.com",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    2, 1, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, false, List.of()
            );

            mockMvc.perform(post("/api/reservations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/reservations/{id}")
    class GetReservationEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Debe retornar reserva si existe")
        void shouldReturnReservation() throws Exception {
            UUID id = UUID.randomUUID();
            Reservation reservation = new Reservation();
            reservation.setId(id);
            reservation.setVisitDate(LocalDate.of(2025, 9, 15));
            reservation.setFirstName("Juan");
            reservation.setLastName("Perez");
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setCircuit(Circuit.A);

            when(reservationService.findById(id)).thenReturn(reservation);

            mockMvc.perform(get("/api/reservations/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("Debe retornar 404 si no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(reservationService.findById(id))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

            mockMvc.perform(get("/api/reservations/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }
}
