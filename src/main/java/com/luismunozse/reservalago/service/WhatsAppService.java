package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Reservation;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${app.whatsapp.account-sid:}")
    private String accountSid;

    @Value("${app.whatsapp.auth-token:}")
    private String authToken;

    @Value("${app.whatsapp.from-number:whatsapp:+14155238886}")
    private String fromNumber;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "AR"));

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
            String messageBody = buildConfirmationMessage(reservation);

            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + toNumber),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("WhatsApp confirmation sent to {} for reservation {}. SID: {}",
                    toNumber, reservation.getId(), message.getSid());

        } catch (Exception e) {
            log.error("Failed to send WhatsApp confirmation for reservation {}: {}",
                    reservation.getId(), e.getMessage());
        }
    }

    private String buildConfirmationMessage(Reservation reservation) {
        String firstName = reservation.getFirstName();
        String formattedDate = reservation.getVisitDate().format(DATE_FORMATTER);
        String reservationCode = reservation.getId().toString().substring(0, 8).toUpperCase();
        int totalPeople = reservation.getAdults18Plus() +
                          reservation.getChildren2To17() +
                          reservation.getBabiesLessThan2();
        String circuit = reservation.getCircuit().name();

        return String.format("""
            🌲 *Reserva Confirmada - Lago Escondido*

            Hola %s,

            Tu reserva ha sido confirmada con éxito.

            📅 *Fecha de visita:* %s
            🎫 *Código de reserva:* %s
            👥 *Personas:* %d
            🗺️ *Circuito:* %s

            📍 Recuerda llegar 15 minutos antes de tu horario asignado.

            ¡Te esperamos!
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
