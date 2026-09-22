package co.uptc.materias.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuración CORS explícita para la API.
 * <p>
 * Orígenes permitidos están configurados para desarrollo local.
 * Para producción o integración con el API Gateway, agregar los orígenes
 * correspondientes en la lista {@code allowedOrigins}.
 *
 * <p>
 * <strong>Para añadir el API Gateway:</strong>
 * Agregar su URL (ej. "http://localhost:8080") a la lista de orígenes
 * permitidos.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos — agregar aquí la URL del API Gateway cuando se integre
        config.setAllowedOrigins(List.of(
                "http://localhost:8080", // Futuro API Gateway
                "http://localhost:3001" // Swagger UI local
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
