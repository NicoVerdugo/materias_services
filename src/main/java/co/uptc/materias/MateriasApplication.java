package co.uptc.materias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio Materias.
 * <p>
 * Gestiona Materias, Docentes y Cursos como API REST independiente,
 * preparada para integración futura con API Gateway.
 */
@SpringBootApplication
public class MateriasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateriasApplication.class, args);
    }
}
