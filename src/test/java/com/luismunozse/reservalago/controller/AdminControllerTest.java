package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
// TestSecurityConfig is in the same package
import com.luismunozse.reservalago.dto.AdminReservationDTO;
import com.luismunozse.reservalago.dto.AdminVisitorDTO;
import com.luismunozse.reservalago.dto.CapacityRequest;
import com.luismunozse.reservalago.dto.CreateEventRequest;
import com.luismunozse.reservalago.dto.EducationalReservationsRequest;
import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.repo.AvailabilityRuleRepository;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.ReservationService;
import com.luismunozse.reservalago.service.SystemConfigService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AvailabilityRuleRepository availabilityRuleRepository;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private SystemConfigService systemConfigService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("PUT /api/admin/availability/{date}")
    class UpsertCapacity {

        @Test
        @DisplayName("Debe crear capacidad para fecha nueva")
        void shouldCreateCapacityForNewDate() throws Exception {
            LocalDate date = LocalDate.of(2025, 9, 15);
            CapacityRequest request = new CapacityRequest();
            request.setCapacity(50);

            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/admin/availability/{date}", date)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(availabilityRuleRepository).save(any(AvailabilityRule.class));
        }

        @Test
        @DisplayName("Debe actualizar capacidad existente")
        void shouldUpdateExistingCapacity() throws Exception {
            LocalDate date = LocalDate.of(2025, 9, 15);
            CapacityRequest request = new CapacityRequest();
            request.setCapacity(100);

            AvailabilityRule existingRule = new AvailabilityRule();
            existingRule.setDay(date);
            existingRule.setCapacity(50);

            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.of(existingRule));

            mockMvc.perform(put("/api/admin/availability/{date}", date)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(availabilityRuleRepository).save(any(AvailabilityRule.class));
        }

        @Test
        @DisplayName("Debe rechazar capacidad negativa")
        void shouldRejectNegativeCapacity() throws Exception {
            LocalDate date = LocalDate.of(2025, 9, 15);
            CapacityRequest request = new CapacityRequest();
            request.setCapacity(-10);

            mockMvc.perform(put("/api/admin/availability/{date}", date)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe rechazar capacidad nula")
        void shouldRejectNullCapacity() throws Exception {
            LocalDate date = LocalDate.of(2025, 9, 15);

            mockMvc.perform(put("/api/admin/availability/{date}", date)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/reservations/export")
    class ExportReservations {

        @Test
        @DisplayName("Debe exportar Excel correctamente")
        void shouldExportExcelSuccessfully() throws Exception {
            byte[] excelData = "fake excel data".getBytes();
            when(reservationService.exportExcel(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                    .thenReturn(excelData);

            mockMvc.perform(get("/api/admin/reservations/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"reservas.xlsx\""))
                    .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @DisplayName("Debe generar nombre de archivo con fecha")
        void shouldGenerateFilenameWithDate() throws Exception {
            LocalDate date = LocalDate.of(2025, 9, 15);
            byte[] excelData = "fake excel data".getBytes();
            when(reservationService.exportExcel(eq(date), any(), any(), any(), any(), any(), any(), eq(false)))
                    .thenReturn(excelData);

            mockMvc.perform(get("/api/admin/reservations/export")
                            .param("date", "2025-09-15"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"reservas_2025-09-15.xlsx\""));
        }

        @Test
        @DisplayName("Debe generar nombre de archivo con mes")
        void shouldGenerateFilenameWithMonth() throws Exception {
            byte[] excelData = "fake excel data".getBytes();
            when(reservationService.exportExcel(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                    .thenReturn(excelData);

            mockMvc.perform(get("/api/admin/reservations/export")
                            .param("month", "2025-09"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"reservas_2025-09.xlsx\""));
        }

        @Test
        @DisplayName("Debe generar nombre de archivo con año")
        void shouldGenerateFilenameWithYear() throws Exception {
            byte[] excelData = "fake excel data".getBytes();
            when(reservationService.exportExcel(any(), any(), eq(2025), any(), any(), any(), any(), eq(false)))
                    .thenReturn(excelData);

            mockMvc.perform(get("/api/admin/reservations/export")
                            .param("year", "2025"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"reservas_2025.xlsx\""));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/reservations")
    class ListReservations {

        @Test
        @DisplayName("Debe retornar página de reservas")
        void shouldReturnPageOfReservations() throws Exception {
            AdminReservationDTO reservation = new AdminReservationDTO(
                    UUID.randomUUID(),
                    LocalDate.of(2025, 9, 15),
                    "Juan",
                    "Pérez",
                    2, 1, 0,
                    "juan@test.com",
                    "1155667788",
                    null,
                    "A",
                    "INDIVIDUAL",
                    "CABA",
                    "CONFIRMED",
                    Instant.now(),
                    "12345678",
                    0,
                    null,
                    List.of()
            );

            when(reservationService.adminListPaged(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(reservation)));

            mockMvc.perform(get("/api/admin/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].firstName").value("Juan"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Debe filtrar por fecha")
        void shouldFilterByDate() throws Exception {
            LocalDate filterDate = LocalDate.of(2025, 9, 15);
            when(reservationService.adminListPaged(eq(filterDate), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/admin/reservations")
                            .param("date", "2025-09-15"))
                    .andExpect(status().isOk());

            verify(reservationService).adminListPaged(eq(filterDate), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Debe filtrar por estado")
        void shouldFilterByStatus() throws Exception {
            when(reservationService.adminListPaged(any(), eq(ReservationStatus.CONFIRMED), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/admin/reservations")
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk());

            verify(reservationService).adminListPaged(any(), eq(ReservationStatus.CONFIRMED), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Debe filtrar por DNI")
        void shouldFilterByDni() throws Exception {
            when(reservationService.adminListPaged(any(), any(), eq("12345678"), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/admin/reservations")
                            .param("dni", "12345678"))
                    .andExpect(status().isOk());

            verify(reservationService).adminListPaged(any(), any(), eq("12345678"), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Debe retornar página vacía si no hay reservas")
        void shouldReturnEmptyPage() throws Exception {
            when(reservationService.adminListPaged(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/admin/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/reservations/{id}/confirm")
    class ConfirmReservation {

        @Test
        @DisplayName("Debe confirmar reserva correctamente")
        void shouldConfirmReservation() throws Exception {
            UUID reservationId = UUID.randomUUID();
            doNothing().when(reservationService).confirmReservation(reservationId);

            mockMvc.perform(post("/api/admin/reservations/{id}/confirm", reservationId))
                    .andExpect(status().isOk());

            verify(reservationService).confirmReservation(reservationId);
        }

        @Test
        @DisplayName("Debe propagar error si reserva no existe")
        void shouldPropagateErrorIfNotFound() throws Exception {
            UUID reservationId = UUID.randomUUID();
            doThrow(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Reserva no encontrada"))
                    .when(reservationService).confirmReservation(reservationId);

            mockMvc.perform(post("/api/admin/reservations/{id}/confirm", reservationId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/reservations/{id}/cancel")
    class CancelReservation {

        @Test
        @DisplayName("Debe cancelar reserva correctamente")
        void shouldCancelReservation() throws Exception {
            UUID reservationId = UUID.randomUUID();
            doNothing().when(reservationService).cancelReservation(reservationId);

            mockMvc.perform(post("/api/admin/reservations/{id}/cancel", reservationId))
                    .andExpect(status().isOk());

            verify(reservationService).cancelReservation(reservationId);
        }

        @Test
        @DisplayName("Debe propagar error si reserva no existe")
        void shouldPropagateErrorIfNotFound() throws Exception {
            UUID reservationId = UUID.randomUUID();
            doThrow(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Reserva no encontrada"))
                    .when(reservationService).cancelReservation(reservationId);

            mockMvc.perform(post("/api/admin/reservations/{id}/cancel", reservationId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/eventos")
    class CreateEvent {

        @Test
        @DisplayName("Debe crear evento correctamente")
        void shouldCreateEventSuccessfully() throws Exception {
            UUID eventId = UUID.randomUUID();
            CreateEventRequest request = new CreateEventRequest(
                    "Taller de fotografía",
                    Instant.parse("2025-11-15T14:00:00Z"),
                    "A",
                    30,
                    "Evento especial"
            );

            when(reservationService.createEvent(any(CreateEventRequest.class))).thenReturn(eventId);

            mockMvc.perform(post("/api/admin/eventos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId.toString()));
        }

        @Test
        @DisplayName("Debe crear evento con campos mínimos")
        void shouldCreateEventWithMinimalFields() throws Exception {
            UUID eventId = UUID.randomUUID();
            CreateEventRequest request = new CreateEventRequest(
                    "Evento básico",
                    Instant.parse("2025-11-15T10:00:00Z"),
                    null,
                    null,
                    null
            );

            when(reservationService.createEvent(any(CreateEventRequest.class))).thenReturn(eventId);

            mockMvc.perform(post("/api/admin/eventos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/config/educational-reservations")
    class GetEducationalConfig {

        @Test
        @DisplayName("Debe retornar estado habilitado")
        void shouldReturnEnabledStatus() throws Exception {
            when(systemConfigService.isEducationalReservationsEnabled()).thenReturn(true);

            mockMvc.perform(get("/api/admin/config/educational-reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("Debe retornar estado deshabilitado")
        void shouldReturnDisabledStatus() throws Exception {
            when(systemConfigService.isEducationalReservationsEnabled()).thenReturn(false);

            mockMvc.perform(get("/api/admin/config/educational-reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/config/educational-reservations")
    class ToggleEducationalConfig {

        @Test
        @DisplayName("Debe habilitar reservas educativas")
        void shouldEnableEducationalReservations() throws Exception {
            EducationalReservationsRequest request = new EducationalReservationsRequest();
            request.setEnabled(true);

            mockMvc.perform(put("/api/admin/config/educational-reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(systemConfigService).setEducationalReservationsEnabled(true);
        }

        @Test
        @DisplayName("Debe deshabilitar reservas educativas")
        void shouldDisableEducationalReservations() throws Exception {
            EducationalReservationsRequest request = new EducationalReservationsRequest();
            request.setEnabled(false);

            mockMvc.perform(put("/api/admin/config/educational-reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));

            verify(systemConfigService).setEducationalReservationsEnabled(false);
        }

        @Test
        @DisplayName("Debe rechazar request sin campo enabled")
        void shouldRejectRequestWithoutEnabled() throws Exception {
            mockMvc.perform(put("/api/admin/config/educational-reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
