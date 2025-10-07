package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationStatus;
import com.luismunozse.reservalago.model.VisitorType;
import com.luismunozse.reservalago.model.Circuit;
import com.luismunozse.reservalago.model.HowHeard;
import com.luismunozse.reservalago.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;

@Tag(name = "Email Test")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @Operation(summary = "Probar template de email")
    @GetMapping("/email-preview")
    public String previewEmail() {
        // Crear una reserva de prueba
        Reservation testReservation = new Reservation();
        testReservation.setId(java.util.UUID.randomUUID());
        testReservation.setVisitDate(LocalDate.now().plusDays(7));
        testReservation.setFirstName("Juan");
        testReservation.setLastName("Pérez");
        testReservation.setDni("12345678");
        testReservation.setPhone("+54 9 351 123-4567");
        testReservation.setEmail("juan.perez@example.com");
        testReservation.setCircuit(Circuit.A);
        testReservation.setVisitorType(VisitorType.INDIVIDUAL);
        testReservation.setAdults18Plus(2);
        testReservation.setChildren2To17(1);
        testReservation.setBabiesLessThan2(0);
        testReservation.setReducedMobility(0);
        testReservation.setAllergies(0);
        testReservation.setComment("Esta es una reserva de prueba");
        testReservation.setOriginLocation("Córdoba, AR");
        testReservation.setHowHeard(HowHeard.WEBSITE);
        testReservation.setAcceptedPolicies(true);
        testReservation.setStatus(ReservationStatus.PENDING);
        testReservation.setCreatedAt(Instant.now());

        try {
            // Generar el HTML del email
            String htmlContent = generateTestEmailHtml(testReservation);
            return htmlContent;
        } catch (Exception e) {
            return "Error generando email: " + e.getMessage();
        }
    }

    @Operation(summary = "Enviar email de prueba")
    @PostMapping("/send-test-email")
    public String sendTestEmail(@RequestParam String email) {
        try {
            // Crear una reserva de prueba
            Reservation testReservation = new Reservation();
            testReservation.setId(java.util.UUID.randomUUID());
            testReservation.setVisitDate(LocalDate.now().plusDays(7));
            testReservation.setFirstName("Usuario");
            testReservation.setLastName("Prueba");
            testReservation.setDni("87654321");
            testReservation.setPhone("+54 9 351 987-6543");
            testReservation.setEmail(email);
            testReservation.setCircuit(Circuit.B);
            testReservation.setVisitorType(VisitorType.INDIVIDUAL);
            testReservation.setAdults18Plus(1);
            testReservation.setChildren2To17(0);
            testReservation.setBabiesLessThan2(0);
            testReservation.setReducedMobility(0);
            testReservation.setAllergies(0);
            testReservation.setComment("Email de prueba enviado desde la API");
            testReservation.setOriginLocation("Buenos Aires, AR");
            testReservation.setHowHeard(HowHeard.RECOMMENDATION);
            testReservation.setAcceptedPolicies(true);
            testReservation.setStatus(ReservationStatus.PENDING);
            testReservation.setCreatedAt(Instant.now());

            emailService.sendReservationConfirmation(testReservation);
            return "Email de prueba enviado exitosamente a: " + email;
        } catch (Exception e) {
            return "Error enviando email: " + e.getMessage();
        }
    }

    private String generateTestEmailHtml(Reservation reservation) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <title>Confirmación de Reserva - Lago Escondido</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                
                <div style="background: linear-gradient(135deg, #2c5aa0, #4a90e2); color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="margin: 0; font-size: 24px;">🌲 Lago Escondido</h1>
                    <p style="margin: 10px 0 0 0;">Confirmación de Reserva</p>
                </div>
                
                <div style="background: #ffffff; padding: 30px; border: 1px solid #ddd; border-top: none;">
                    <h2>¡Hola %s!</h2>
                    
                    <p>Te confirmamos que hemos recibido tu reserva para visitar el Lago Escondido.</p>
                    
                    <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #2c5aa0;">
                        <h3 style="margin-top: 0; color: #2c5aa0;">📋 Detalles de la Reserva</h3>
                        
                        <p><strong>ID de Reserva:</strong> %s</p>
                        <p><strong>Fecha de Visita:</strong> %s</p>
                        <p><strong>Tipo de Visita:</strong> Individual</p>
                        <p><strong>Circuito:</strong> %s</p>
                        <p><strong>Adultos (18+ años):</strong> %d</p>
                        <p><strong>Niños (2-17 años):</strong> %d</p>
                        <p><strong>Bebés (&lt;2 años):</strong> %d</p>
                        <p><strong>Estado:</strong> <span style="background-color: #fff3cd; color: #856404; padding: 4px 8px; border-radius: 4px; font-size: 12px;">PENDIENTE</span></p>
                        <p><strong>Lugar de Origen:</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #e7f3ff; border: 1px solid #b8daff; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <h3 style="margin: 0 0 10px 0; color: #004085;">ℹ️ Información Importante</h3>
                        <ul>
                            <li><strong>Estado Pendiente:</strong> Tu reserva está siendo procesada. Recibirás una confirmación definitiva próximamente.</li>
                            <li><strong>Horario de Visita:</strong> Consulta los horarios disponibles en nuestro sitio web.</li>
                            <li><strong>Requisitos:</strong> Presenta este email y un documento de identidad el día de tu visita.</li>
                        </ul>
                    </div>
                    
                    <div style="background-color: #e7f3ff; border: 1px solid #b8daff; border-radius: 8px; padding: 20px; margin: 20px 0;">
                        <h3 style="margin: 0 0 10px 0; color: #004085;">💬 Comentarios</h3>
                        <p>%s</p>
                    </div>
                </div>
                
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 14px; border-radius: 0 0 8px 8px; border: 1px solid #ddd; border-top: none;">
                    <p><strong>Lago Escondido</strong></p>
                    <p>📧 <a href="mailto:info@lago-escondido.com" style="color: #2c5aa0;">info@lago-escondido.com</a></p>
                    <p>📞 +54 9 351 XXX-XXXX</p>
                    <p>🌐 <a href="https://lago-escondido.com" style="color: #2c5aa0;">lago-escondido.com</a></p>
                    <p style="margin-top: 20px; font-size: 12px; color: #adb5bd;">
                        Este es un email automático, por favor no respondas a este mensaje.
                    </p>
                </div>
                
            </body>
            </html>
            """.formatted(
                reservation.getFirstName(),
                reservation.getId(),
                reservation.getVisitDate(),
                reservation.getCircuit(),
                reservation.getAdults18Plus(),
                reservation.getChildren2To17(),
                reservation.getBabiesLessThan2(),
                reservation.getOriginLocation(),
                reservation.getComment()
            );
    }
}

