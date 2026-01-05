package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.repo.UserRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppService {

    private final UserRepository userRepository;

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${app.whatsapp.account-sid:}")
    private String accountSid;

    @Value("${app.whatsapp.auth-token:}")
    private String authToken;

    @Value("${app.whatsapp.from-number:whatsapp:+14155238886}")
    private String fromNumber;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // Content SID de los templates aprobados en Twilio
    private static final String CONFIRMATION_TEMPLATE_SID = "HX28e149d6dc3e7a34a5377c68d83d8cb0";
    private static final String CANCELLATION_TEMPLATE_SID = "HX025c05b19133c95145e2542d5be279e0";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.of("es", "AR"));

    @PostConstruct
    public void init() {
        if (enabled && !accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("WhatsApp service initialized with Twilio");
        } else {
            log.warn("WhatsApp service is disabled or credentials are missing");
        }
    }

    @Async
    public void sendConfirmation(Reservation reservation) {
        if (!enabled) {
            log.debug("WhatsApp disabled, skipping notification for reservation {}", reservation.getId());
            return;
        }

        if (accountSid.isBlank() || authToken.isBlank()) {
            log.warn("Twilio credentials not configured, skipping WhatsApp notification");
            return;
        }

        try {
            String toNumber = normalizePhoneNumber(reservation.getPhone());

            // Variable para el template: {{1}} = link reserva
            String reservationUrl = frontendUrl + "/reserva/" + reservation.getId();
            String contentVariables = String.format("{\"1\":\"%s\"}", reservationUrl);

            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + toNumber),
                    new PhoneNumber(fromNumber),
                    "" // Body vacío cuando se usa template
            )
            .setContentSid(CONFIRMATION_TEMPLATE_SID)
            .setContentVariables(contentVariables)
            .create();

            log.info("WhatsApp confirmation sent to {} for reservation {}. SID: {}",
                    toNumber, reservation.getId(), message.getSid());

        } catch (Exception e) {
            log.error("Failed to send WhatsApp confirmation for reservation {}: {}",
                    reservation.getId(), e.getMessage());
        }
    }

    @Async
    public void sendCancellation(Reservation reservation) {
        if (!enabled) {
            log.debug("WhatsApp disabled, skipping cancellation notification for reservation {}", reservation.getId());
            return;
        }

        if (accountSid.isBlank() || authToken.isBlank()) {
            log.warn("Twilio credentials not configured, skipping WhatsApp cancellation notification");
            return;
        }

        try {
            String toNumber = normalizePhoneNumber(reservation.getPhone());

            // Template sin variables
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + toNumber),
                    new PhoneNumber(fromNumber),
                    "" // Body vacío cuando se usa template
            )
            .setContentSid(CANCELLATION_TEMPLATE_SID)
            .create();

            log.info("WhatsApp cancellation sent to {} for reservation {}. SID: {}",
                    toNumber, reservation.getId(), message.getSid());

        } catch (Exception e) {
            log.error("Failed to send WhatsApp cancellation for reservation {}: {}",
                    reservation.getId(), e.getMessage());
        }
    }

    @Async
    public void sendAdminNotification(Reservation reservation) {
        if (!enabled) {
            log.debug("WhatsApp disabled, skipping admin notification for reservation {}", reservation.getId());
            return;
        }

        if (accountSid.isBlank() || authToken.isBlank()) {
            log.warn("Twilio credentials not configured, skipping admin notification");
            return;
        }

        List<String> phones = userRepository.findAdminPhones();

        if (phones.isEmpty()) {
            log.debug("No admin phones found in database, skipping admin notification");
            return;
        }

        String messageBody = buildAdminNotificationMessage(reservation);

        for (String phone : phones) {
            try {
                String toNumber = normalizePhoneNumber(phone);

                Message message = Message.creator(
                        new PhoneNumber("whatsapp:" + toNumber),
                        new PhoneNumber(fromNumber),
                        messageBody
                ).create();

                log.info("WhatsApp admin notification sent to {} for reservation {}. SID: {}",
                        toNumber, reservation.getId(), message.getSid());

            } catch (Exception e) {
                log.error("Failed to send WhatsApp admin notification to {} for reservation {}: {}",
                        phone, reservation.getId(), e.getMessage());
            }
        }
    }

    private String buildAdminNotificationMessage(Reservation reservation) {
        String fullName = reservation.getFirstName() + " " + reservation.getLastName();
        String formattedDate = reservation.getVisitDate().format(DATE_FORMATTER);
        String reservationCode = reservation.getId().toString().substring(0, 8).toUpperCase();
        int totalPeople = reservation.getAdults18Plus() +
                          reservation.getChildren2To17() +
                          reservation.getBabiesLessThan2();
        String circuit = reservation.getCircuit().name();
        String adminUrl = frontendUrl + "/admin";

        return String.format("""
            🔔 *Nueva Reserva Pendiente*

            Se ha registrado una nueva reserva que requiere revisión.

            👤 *Solicitante:* %s
            📅 *Fecha de visita:* %s
            🎫 *Código:* %s
            👥 *Personas:* %d
            🗺️ *Circuito:* %s
            📞 *Teléfono:* %s

            🔗 *Acceder al panel:* %s
            """, fullName, formattedDate, reservationCode, totalPeople, circuit, reservation.getPhone(), adminUrl);
    }

    private String buildConfirmationMessage(Reservation reservation) {
        String firstName = reservation.getFirstName();
        String formattedDate = reservation.getVisitDate().format(DATE_FORMATTER);
        String reservationCode = reservation.getId().toString().substring(0, 8).toUpperCase();
        int totalPeople = reservation.getAdults18Plus() +
                          reservation.getChildren2To17() +
                          reservation.getBabiesLessThan2();
        String circuit = reservation.getCircuit().name();
        String reservationDetailUrl = frontendUrl + "/reserva/" + reservation.getId();

        return String.format("""
            🌲 *Reserva Confirmada - Lago Escondido*

            Hola %s,

            Tu reserva ha sido confirmada con éxito.

            📅 *Fecha de visita:* %s
            🎫 *Código de reserva:* %s
            👥 *Personas:* %d
            🗺️ *Circuito:* %s

            📍 Recuerda llegar 15 minutos antes de tu horario asignado.

            🔗 *Ver detalle de tu reserva:* %s

            ¡Te esperamos!
            Equipo Lago Escondido
            """, firstName, formattedDate, reservationCode, totalPeople, circuit, reservationDetailUrl);
    }

    private String buildCancellationMessage(Reservation reservation) {
        String firstName = reservation.getFirstName();
        String formattedDate = reservation.getVisitDate().format(DATE_FORMATTER);
        String reservationCode = reservation.getId().toString().substring(0, 8).toUpperCase();
        int totalPeople = reservation.getAdults18Plus() +
                          reservation.getChildren2To17() +
                          reservation.getBabiesLessThan2();
        String circuit = reservation.getCircuit().name();

        return String.format("""
            ❌ *Reserva Cancelada - Lago Escondido*

            Hola %s,

            Lamentamos informarte que tu reserva ha sido cancelada.

            📅 *Fecha de visita:* %s
            🎫 *Código de reserva:* %s
            👥 *Personas:* %d
            🗺️ *Circuito:* %s

            Si tienes alguna consulta, no dudes en contactarnos.

            Equipo Lago Escondido
            """, firstName, formattedDate, reservationCode, totalPeople, circuit);
    }

    private String normalizePhoneNumber(String phone) {
        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");

        // Handle Argentine mobile numbers for WhatsApp
        // WhatsApp Argentina format: +549XXXXXXXXXX (9 is required for mobile)

        if (digits.startsWith("549") && digits.length() == 13) {
            // Already correct format: 5493517734676
            return "+" + digits;
        } else if (digits.startsWith("54") && digits.length() == 12) {
            // Has country code but missing 9: 543517734676 -> +5493517734676
            return "+549" + digits.substring(2);
        } else if (digits.startsWith("0")) {
            // Local format with leading 0: 03517734676 -> +5493517734676
            return "+549" + digits.substring(1);
        } else if (digits.length() == 10) {
            // 10 digits (area code + number): 3517734676 -> +5493517734676
            return "+549" + digits;
        } else if (digits.startsWith("9") && digits.length() == 11) {
            // Mobile with 9 prefix: 93517734676 -> +5493517734676
            return "+54" + digits;
        }

        // Default: assume it's a complete international number
        return "+" + digits;
    }
}
