package co.uptc.materias.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger UI.
 * <p>
 * Swagger UI disponible en: http://localhost:3001/swagger-ui/index.html
 * OpenAPI JSON en: http://localhost:3001/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Materias Service API")
                                                .description("Microservicio de gestión de Materias, Docentes y Cursos. "
                                                                + "Laboratorio 3 — Arquitectura Modular con API Gateway. "
                                                                + "Sistemas Distribuidos, UPTC.")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("UPTC — Ingeniería de Sistemas")
                                                                .url("https://www.uptc.edu.co")));
        }
}
