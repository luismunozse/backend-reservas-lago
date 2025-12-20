package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateReservationRequest;
import com.luismunozse.reservalago.dto.VisitorDTO;
import com.luismunozse.reservalago.model.*;
import com.luismunozse.reservalago.repo.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ReservationExcelExporter reservationExcelExporter;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private ReservationService reservationService;

    private CreateReservationRequest validRequest;
    private Reservation mockReservation;

    @BeforeEach
    void setUp() {
        validRequest = new CreateReservationRequest(
                LocalDate.now().plusDays(7),
                "Juan",
                "Perez",
                "12345678",
                "1155667788",
                "juan@test.com",
                Circuit.A,
                VisitorType.INDIVIDUAL,
                null,
                null,
                2,
                1,
                0,
                0,
                null,
                "Buenos Aires",
                HowHeard.SOCIAL,
                true,
                List.of()
        );

        mockReservation = new Reservation();
        mockReservation.setId(UUID.randomUUID());
        mockReservation.setStatus(ReservationStatus.PENDING);
    }

    @Nested
    @DisplayName("create() - Validaciones")
    class CreateValidations {

        @Test
        @DisplayName("Debe rechazar reservas para fechas pasadas")
        void shouldRejectPastDates() {
            var pastRequest = new CreateReservationRequest(
                    LocalDate.now().minusDays(1),
                    "Juan", "Perez", "12345678", "1155667788", "juan@test.com",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    2, 0, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, true, List.of()
            );

            assertThatThrownBy(() -> reservationService.create(pastRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("fechas pasadas");
        }

        @Test
        @DisplayName("Debe rechazar reservas sin personas")
        void shouldRejectZeroPeople() {
            var zeroRequest = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Juan", "Perez", "12345678", "1155667788", "juan@test.com",
                    Circuit.A, VisitorType.INDIVIDUAL, null, null,
                    0, 0, 0, 0, null, "Buenos Aires", HowHeard.SOCIAL, true, List.of()
            );

            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(30);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);

            assertThatThrownBy(() -> reservationService.create(zeroRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("al menos una persona");
        }

        @Test
        @DisplayName("Debe rechazar cuando no hay cupo disponible")
        void shouldRejectWhenNoCapacity() {
            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(30);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(29);

            assertThatThrownBy(() -> reservationService.create(validRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No hay cupo");
        }

        @Test
        @DisplayName("Debe rechazar reservas duplicadas (mismo DNI y fecha)")
        void shouldRejectDuplicateReservation() {
            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(30);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);
            when(reservationRepository.existsByVisitDateAndDniAndStatusNot(
                    any(), eq("12345678"), eq(ReservationStatus.CANCELLED)))
                    .thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(validRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Ya existe una visita");
        }
    }

    @Nested
    @DisplayName("create() - Instituciones Educativas")
    class EducationalInstitutions {

        @Test
        @DisplayName("Debe rechazar institución educativa si están deshabilitadas")
        void shouldRejectWhenEducationalDisabled() {
            var eduRequest = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Director", "Escuela", "12345678", "1155667788", "dir@escuela.com",
                    Circuit.A, VisitorType.EDUCATIONAL_INSTITUTION, "Escuela N°1", 25,
                    2, 0, 0, 0, null, "Córdoba", HowHeard.SOCIAL, true, List.of()
            );

            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(100);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);
            when(systemConfigService.isEducationalReservationsEnabled()).thenReturn(false);

            assertThatThrownBy(() -> reservationService.create(eduRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("instituciones educativas no están habilitadas");
        }

        @Test
        @DisplayName("Debe rechazar institución educativa sin nombre")
        void shouldRejectWithoutInstitutionName() {
            var eduRequest = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Director", "Escuela", "12345678", "1155667788", "dir@escuela.com",
                    Circuit.A, VisitorType.EDUCATIONAL_INSTITUTION, null, 25,
                    2, 0, 0, 0, null, "Córdoba", HowHeard.SOCIAL, true, List.of()
            );

            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(100);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);
            when(systemConfigService.isEducationalReservationsEnabled()).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(eduRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("institutionName");
        }

        @Test
        @DisplayName("Debe rechazar institución educativa sin cantidad de estudiantes")
        void shouldRejectWithoutStudentCount() {
            var eduRequest = new CreateReservationRequest(
                    LocalDate.now().plusDays(7),
                    "Director", "Escuela", "12345678", "1155667788", "dir@escuela.com",
                    Circuit.A, VisitorType.EDUCATIONAL_INSTITUTION, "Escuela N°1", null,
                    2, 0, 0, 0, null, "Córdoba", HowHeard.SOCIAL, true, List.of()
            );

            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(100);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);
            when(systemConfigService.isEducationalReservationsEnabled()).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(eduRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("institutionStudents");
        }
    }

    @Nested
    @DisplayName("create() - Caso exitoso")
    class CreateSuccess {

        @Test
        @DisplayName("Debe crear reserva correctamente y notificar por WhatsApp")
        void shouldCreateReservationAndNotify() {
            when(reservationMapper.normalizeDni("12345678")).thenReturn("12345678");
            when(availabilityService.capacityFor(any())).thenReturn(30);
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);
            when(reservationRepository.existsByVisitDateAndDniAndStatusNot(any(), any(), any()))
                    .thenReturn(false);
            when(reservationMapper.fromCreateRequest(any(), eq("12345678")))
                    .thenReturn(mockReservation);

            UUID result = reservationService.create(validRequest);

            assertThat(result).isEqualTo(mockReservation.getId());
            verify(reservationRepository).save(mockReservation);
            verify(whatsAppService).sendAdminNotification(mockReservation);
        }
    }

    @Nested
    @DisplayName("confirmReservation()")
    class ConfirmReservation {

        @Test
        @DisplayName("Debe confirmar reserva y enviar notificación")
        void shouldConfirmAndNotify() {
            UUID id = UUID.randomUUID();
            Reservation reservation = new Reservation();
            reservation.setId(id);
            reservation.setStatus(ReservationStatus.PENDING);

            when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

            reservationService.confirmReservation(id);

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(reservationRepository).save(reservation);
            verify(whatsAppService).sendConfirmation(reservation);
        }

        @Test
        @DisplayName("Debe lanzar excepción si la reserva no existe")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(reservationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.confirmReservation(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrada");
        }
    }

    @Nested
    @DisplayName("cancelReservation()")
    class CancelReservation {

        @Test
        @DisplayName("Debe cancelar reserva y enviar notificación")
        void shouldCancelAndNotify() {
            UUID id = UUID.randomUUID();
            Reservation reservation = new Reservation();
            reservation.setId(id);
            reservation.setStatus(ReservationStatus.CONFIRMED);

            when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

            reservationService.cancelReservation(id);

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(reservationRepository).save(reservation);
            verify(whatsAppService).sendCancellation(reservation);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Debe retornar reserva si existe")
        void shouldReturnWhenExists() {
            UUID id = mockReservation.getId();
            when(reservationRepository.findById(id)).thenReturn(Optional.of(mockReservation));

            Reservation result = reservationService.findById(id);

            assertThat(result).isEqualTo(mockReservation);
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(reservationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.findById(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrada");
        }
    }
}
