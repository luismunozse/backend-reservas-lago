package com.luismunozse.reservalago.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Reserva Lago Escondido - API",
                version = "v1.0.0",
                description = """
                        API REST para el sistema de reservas del Lago Escondido.
                        
                        ## Funcionalidades
                        - ✅ Consulta de disponibilidad por fecha y mes
                        - ✅ Creación de reservas individuales e institucionales
                        - ✅ Administración de reservas (confirmar, cancelar)
                        - ✅ Exportación de datos en CSV
                        - ✅ Gestión de capacidad por día
                        
                        ## Autenticación
                        Los endpoints de administración requieren autenticación básica:
                        - Usuario: `admin`
                        - Contraseña: `admin123`
                        
                        ## Estados de Reserva
                        - `PENDING`: Reserva pendiente de confirmación
                        - `CONFIRMED`: Reserva confirmada
                        - `CANCELLED`: Reserva cancelada
                        """,
                contact = @Contact(
                        name = "Equipo de Lago Escondido", 
                        email = "soporte@lago-escondido.com",
                        url = "https://lago-escondido.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor de Desarrollo"),
                @Server(url = "https://api.lago-escondido.com", description = "Servidor de Producción")
        },
        security = @SecurityRequirement(name = "basicAuth"),
        tags = {
                @Tag(name = "Disponibilidad", description = "Consultas de disponibilidad y capacidad"),
                @Tag(name = "Reservas", description = "Gestión de reservas públicas"),
                @Tag(name = "Administración", description = "Endpoints administrativos (requiere autenticación)")
        }
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Autenticación básica para endpoints administrativos"
)

@Configuration
public class OpenApiConfig {

    // Grupo público: endpoints sin autenticación
    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("01 - Público")
                .displayName("🌐 API Pública")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**")
                .build();
    }

    // Grupo admin: endpoints que requieren autenticación
    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("02 - Administración")
                .displayName("🔐 API Administrativa")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    // Grupo completo: todos los endpoints
    @Bean
    GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("00 - Completa")
                .displayName("📚 API Completa")
                .pathsToMatch("/api/**")
                .build();
    }
}
