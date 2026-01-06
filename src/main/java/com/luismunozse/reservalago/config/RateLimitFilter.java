package com.luismunozse.reservalago.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Rate Limiting basado en IP usando Bucket4j.
 * Protege endpoints públicos contra abuso.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Límites por endpoint (requests por minuto)
    private static final int LIMIT_RESERVATIONS = 3;
    private static final int LIMIT_LOGIN = 5;
    private static final int LIMIT_AVAILABILITY = 15;
    private static final int LIMIT_DEFAULT = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIP(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Solo aplicar rate limit a endpoints específicos
        if (!shouldRateLimit(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = ip + ":" + getEndpointKey(path, method);
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(path, method));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {} {}", ip, method, path);
            sendRateLimitResponse(response);
        }
    }

    /**
     * Determina si el endpoint debe tener rate limiting
     */
    private boolean shouldRateLimit(String path, String method) {
        // POST /api/reservations - crear reserva
        if ("POST".equals(method) && path.equals("/api/reservations")) {
            return true;
        }
        // POST /api/auth/login - login
        if ("POST".equals(method) && path.equals("/api/auth/login")) {
            return true;
        }
        // GET /api/availability - consultar disponibilidad
        if ("GET".equals(method) && path.equals("/api/availability")) {
            return true;
        }
        return false;
    }

    /**
     * Genera una clave única para el bucket según el endpoint
     */
    private String getEndpointKey(String path, String method) {
        if ("POST".equals(method) && path.equals("/api/reservations")) {
            return "reservations";
        }
        if ("POST".equals(method) && path.equals("/api/auth/login")) {
            return "login";
        }
        if ("GET".equals(method) && path.equals("/api/availability")) {
            return "availability";
        }
        return "default";
    }

    /**
     * Crea un bucket con el límite apropiado según el endpoint
     */
    private Bucket createBucket(String path, String method) {
        int limit = LIMIT_DEFAULT;

        if ("POST".equals(method) && path.equals("/api/reservations")) {
            limit = LIMIT_RESERVATIONS;
        } else if ("POST".equals(method) && path.equals("/api/auth/login")) {
            limit = LIMIT_LOGIN;
        } else if ("GET".equals(method) && path.equals("/api/availability")) {
            limit = LIMIT_AVAILABILITY;
        }

        return Bucket.builder()
                .addLimit(Bandwidth.simple(limit, Duration.ofMinutes(1)))
                .build();
    }

    /**
     * Obtiene la IP real del cliente, considerando proxies
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For puede contener múltiples IPs, la primera es el cliente real
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Envía respuesta 429 Too Many Requests
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"Demasiadas solicitudes. Intente nuevamente en 1 minuto.\"}");
    }
}
