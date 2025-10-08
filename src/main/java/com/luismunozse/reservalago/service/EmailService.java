package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.fromName}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Envía email de confirmación de reserva
     */
    public void sendReservationConfirmation(Reservation reservation) {
        if (!mailEnabled) {
            log.info("Email deshabilitado, saltando envío para reserva: {}", reservation.getId());
            return;
        }

        try {
            String subject = String.format("Confirmación de Reserva - Lago Escondido - %s", 
                reservation.getVisitDate().toString());
            
            String htmlContent = generateReservationEmail(reservation);
            
            sendHtmlEmail(reservation.getEmail(), subject, htmlContent);
            
            log.info("Email de confirmación enviado exitosamente a: {}", reservation.getEmail());
            
        } catch (Exception e) {
            log.error("Error enviando email de confirmación para reserva {}: {}", 
                reservation.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envía email de cancelación de reserva
     */
    public void sendReservationCancellation(Reservation reservation) {
        if (!mailEnabled) {
            log.info("Email deshabilitado, saltando envío de cancelación para reserva: {}", reservation.getId());
            return;
        }

        try {
            String subject = String.format("Cancelación de Reserva - Lago Escondido - %s", 
                reservation.getVisitDate().toString());
            
            String htmlContent = generateCancellationEmail(reservation);
            
            sendHtmlEmail(reservation.getEmail(), subject, htmlContent);
            
            log.info("Email de cancelación enviado exitosamente a: {}", reservation.getEmail());
            
        } catch (Exception e) {
            log.error("Error enviando email de cancelación para reserva {}: {}", 
                reservation.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envía email HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        
        // Crear versión de texto plano
        String textContent = createPlainTextVersion(htmlContent);
        helper.setText(textContent, htmlContent);
        
        mailSender.send(message);
    }
    
    /**
     * Crea una versión de texto plano del email
     */
    private String createPlainTextVersion(String htmlContent) {
        // Remover etiquetas HTML básicas y crear versión de texto
        return htmlContent
            .replaceAll("<[^>]+>", "") // Remover todas las etiquetas HTML
            .replaceAll("&nbsp;", " ") // Reemplazar espacios no separables
            .replaceAll("&amp;", "&") // Reemplazar ampersands
            .replaceAll("&lt;", "<") // Reemplazar menor que
            .replaceAll("&gt;", ">") // Reemplazar mayor que
            .replaceAll("&quot;", "\"") // Reemplazar comillas
            .replaceAll("\\s+", " ") // Normalizar espacios
            .trim();
    }

    /**
     * Genera el contenido HTML para email de confirmación
     */
    private String generateReservationEmail(Reservation reservation) {
        Context context = new Context();
        context.setVariable("reservation", reservation);
        context.setVariable("visitorTypeText", getVisitorTypeText(reservation.getVisitorType()));
        context.setVariable("circuitText", getCircuitText(reservation.getCircuit()));
        context.setVariable("statusText", getStatusText(reservation.getStatus()));
        
        return templateEngine.process("reservation-confirmation", context);
    }

    /**
     * Genera el contenido HTML para email de cancelación
     */
    private String generateCancellationEmail(Reservation reservation) {
        Context context = new Context();
        context.setVariable("reservation", reservation);
        context.setVariable("visitorTypeText", getVisitorTypeText(reservation.getVisitorType()));
        context.setVariable("circuitText", getCircuitText(reservation.getCircuit()));
        
        return templateEngine.process("reservation-cancellation", context);
    }

    private String getVisitorTypeText(com.luismunozse.reservalago.model.VisitorType visitorType) {
        return switch (visitorType) {
            case INDIVIDUAL -> "Individual";
            case EDUCATIONAL_INSTITUTION -> "Institución Educativa";
            case EVENT -> "Evento";
        };
    }

    private String getCircuitText(com.luismunozse.reservalago.model.Circuit circuit) {
        return switch (circuit) {
            case A -> "Circuito A";
            case B -> "Circuito B";
            case C -> "Circuito C";
            case D -> "Circuito D";
        };
    }

    private String getStatusText(com.luismunozse.reservalago.model.ReservationStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
        };
    }
}
