package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.AvailabilityRule;
import com.luismunozse.reservalago.repo.AvailabilityRuleRepository;
import com.luismunozse.reservalago.repo.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityRuleRepository availabilityRuleRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        // Configurar capacidad por defecto via mock de SystemConfigService
        // lenient() porque algunos tests usan reglas específicas y no llaman a getDefaultCapacity()
        lenient().when(systemConfigService.getDefaultCapacity()).thenReturn(30);
    }

    @Nested
    @DisplayName("availabilityFor()")
    class AvailabilityFor {

        @Test
        @DisplayName("Debe retornar capacidad por defecto cuando no hay regla")
        void shouldReturnDefaultCapacityWhenNoRule() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(0);

            Map<String, Object> result = availabilityService.availabilityFor(date);

            assertThat(result.get("date")).isEqualTo(date);
            assertThat(result.get("capacity")).isEqualTo(30);
            assertThat(result.get("remaining")).isEqualTo(30);
        }

        @Test
        @DisplayName("Debe usar capacidad de la regla cuando existe")
        void shouldUseRuleCapacityWhenExists() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            AvailabilityRule rule = new AvailabilityRule();
            rule.setDay(date);
            rule.setCapacity(50);

            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.of(rule));
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(0);

            Map<String, Object> result = availabilityService.availabilityFor(date);

            assertThat(result.get("capacity")).isEqualTo(50);
            assertThat(result.get("remaining")).isEqualTo(50);
        }

        @Test
        @DisplayName("Debe calcular correctamente lugares restantes")
        void shouldCalculateRemainingCorrectly() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(20);

            Map<String, Object> result = availabilityService.availabilityFor(date);

            assertThat(result.get("capacity")).isEqualTo(30);
            assertThat(result.get("remaining")).isEqualTo(10);
        }

        @Test
        @DisplayName("Remaining no debe ser negativo")
        void shouldNotReturnNegativeRemaining() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(35); // Más que la capacidad

            Map<String, Object> result = availabilityService.availabilityFor(date);

            assertThat(result.get("remaining")).isEqualTo(0);
        }

        @Test
        @DisplayName("Debe manejar día sin reservas")
        void shouldHandleDayWithNoReservations() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(0);

            Map<String, Object> result = availabilityService.availabilityFor(date);

            assertThat(result.get("remaining")).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("availabilityForMonth()")
    class AvailabilityForMonth {

        @Test
        @DisplayName("Debe retornar disponibilidad para todos los días del mes")
        void shouldReturnAvailabilityForAllDaysInMonth() {
            LocalDate september = LocalDate.of(2025, 9, 1);
            when(availabilityRuleRepository.findByDay(any())).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);

            List<Map<String, Object>> result = availabilityService.availabilityForMonth(september);

            // Septiembre tiene 30 días
            assertThat(result).hasSize(30);
        }

        @Test
        @DisplayName("Debe formatear correctamente las fechas")
        void shouldFormatDatesCorrectly() {
            LocalDate september = LocalDate.of(2025, 9, 1);
            when(availabilityRuleRepository.findByDay(any())).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);

            List<Map<String, Object>> result = availabilityService.availabilityForMonth(september);

            assertThat(result.get(0).get("availableDate")).isEqualTo("2025-09-01");
            assertThat(result.get(29).get("availableDate")).isEqualTo("2025-09-30");
        }

        @Test
        @DisplayName("Debe incluir totalCapacity y remainingCapacity")
        void shouldIncludeCapacityFields() {
            LocalDate september = LocalDate.of(2025, 9, 1);
            when(availabilityRuleRepository.findByDay(any())).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(10);

            List<Map<String, Object>> result = availabilityService.availabilityForMonth(september);

            Map<String, Object> firstDay = result.get(0);
            assertThat(firstDay).containsKey("totalCapacity");
            assertThat(firstDay).containsKey("remainingCapacity");
            assertThat(firstDay.get("totalCapacity")).isEqualTo(30);
            assertThat(firstDay.get("remainingCapacity")).isEqualTo(20);
        }

        @Test
        @DisplayName("Debe manejar febrero correctamente (28 días)")
        void shouldHandleFebruaryCorrectly() {
            LocalDate february = LocalDate.of(2025, 2, 1);
            when(availabilityRuleRepository.findByDay(any())).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);

            List<Map<String, Object>> result = availabilityService.availabilityForMonth(february);

            // 2025 no es bisiesto
            assertThat(result).hasSize(28);
        }

        @Test
        @DisplayName("Debe manejar año bisiesto")
        void shouldHandleLeapYear() {
            LocalDate february = LocalDate.of(2024, 2, 1);
            when(availabilityRuleRepository.findByDay(any())).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(any())).thenReturn(0);

            List<Map<String, Object>> result = availabilityService.availabilityForMonth(february);

            // 2024 es bisiesto
            assertThat(result).hasSize(29);
        }
    }

    @Nested
    @DisplayName("capacityFor()")
    class CapacityFor {

        @Test
        @DisplayName("Debe retornar capacidad para una fecha")
        void shouldReturnCapacity() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.empty());
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(0);

            int capacity = availabilityService.capacityFor(date);

            assertThat(capacity).isEqualTo(30);
        }

        @Test
        @DisplayName("Debe retornar capacidad personalizada")
        void shouldReturnCustomCapacity() {
            LocalDate date = LocalDate.of(2025, 9, 15);
            AvailabilityRule rule = new AvailabilityRule();
            rule.setDay(date);
            rule.setCapacity(100);

            when(availabilityRuleRepository.findByDay(date)).thenReturn(Optional.of(rule));
            when(reservationRepository.totalPeopleForDate(date)).thenReturn(0);

            int capacity = availabilityService.capacityFor(date);

            assertThat(capacity).isEqualTo(100);
        }
    }
}
