package co.uptc.materias.integracion;

import co.uptc.materias.dto.request.CursoUpdateRequest;
import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.entity.EstadoCurso;
import co.uptc.materias.entity.Modalidad;
import co.uptc.materias.exception.CupoAgotadoException;
import co.uptc.materias.exception.ReglaNegocioException;
import co.uptc.materias.service.CursoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas contra un PostgreSQL real (el de docker-compose u otro).
 * <p>
 * El proyecto no usa Testcontainers, así que estas pruebas se saltan salvo que se
 * defina {@code MATERIAS_IT_DB_URL}. Flyway aplica V1..V4 al arrancar. Cada prueba
 * crea su propia materia, docente y curso, y al final borra solo esas filas.
 * <pre>
 * MATERIAS_IT_DB_URL=jdbc:postgresql://localhost:5433/materias_db
 * MATERIAS_IT_DB_USER=materias_user
 * MATERIAS_IT_DB_PASSWORD=...
 * mvn test -Dtest=CursoCupoPostgresTest
 * </pre>
 */
@Tag("integracion")
@EnabledIfEnvironmentVariable(named = "MATERIAS_IT_DB_URL", matches = ".+")
@SpringBootTest(properties = {
        // S3 no se usa aquí; valores ficticios para que arranque el contexto
        "aws.access-key-id=test",
        "aws.secret-access-key=test",
        "aws.region=us-east-1",
        "aws.bucket-name=test",
        // Pool mayor que el número de hilos para que todos compitan a la vez en la BD
        "spring.datasource.hikari.maximum-pool-size=25"
})
@DisplayName("Cupo de cursos contra PostgreSQL real")
class CursoCupoPostgresTest {

    private static final int HILOS = 20;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MATERIAS_IT_DB_URL"));
        registry.add("spring.datasource.username",
                () -> envOr("MATERIAS_IT_DB_USER", "materias_user"));
        registry.add("spring.datasource.password",
                () -> envOr("MATERIAS_IT_DB_PASSWORD", "materias_pass"));
    }

    @Autowired
    private CursoService cursoService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long materiaId;
    private Long docenteId;
    private final List<Long> cursosCreados = new ArrayList<>();

    @BeforeEach
    void crearDatos() {
        materiaId = jdbc.queryForObject("""
                INSERT INTO materias (nombre, descripcion, creditos, programa)
                VALUES ('IT concurrencia', 'Creada por CursoCupoPostgresTest', 3, 'Pruebas')
                RETURNING id
                """, Long.class);
        docenteId = jdbc.queryForObject("""
                INSERT INTO docentes (nombres, apellidos, correo_institucional, especialidad, activo)
                VALUES ('IT', 'Concurrencia', ?, 'Pruebas', true) RETURNING id
                """, Long.class, "it-" + UUID.randomUUID() + "@test.local");
    }

    @AfterEach
    void borrarDatos() {
        cursosCreados.forEach(id -> jdbc.update("DELETE FROM cursos WHERE id = ?", id));
        jdbc.update("DELETE FROM docentes WHERE id = ?", docenteId);
        jdbc.update("DELETE FROM materias WHERE id = ?", materiaId);
    }

    @Test
    @DisplayName("cupo 1 y 20 hilos reservando a la vez: 1 éxito, 19 CupoAgotado, cupo_disponible final 0")
    void reservaConcurrente() throws Exception {
        Long cursoId = crearCurso(1);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger agotados = new AtomicInteger();
        List<Throwable> otros = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            tareas.add(pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    cursoService.reservarCupo(cursoId);
                    exitos.incrementAndGet();
                } catch (CupoAgotadoException e) {
                    if (CupoAgotadoException.CUPO_AGOTADO.equals(e.getErrorType())) {
                        agotados.incrementAndGet();
                    } else {
                        otros.add(e);
                    }
                } catch (Throwable e) {
                    otros.add(e);
                }
            }));
        }
        assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
        salida.countDown(); // todos los hilos arrancan al mismo tiempo
        for (Future<?> tarea : tareas) {
            tarea.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(otros).as("excepciones inesperadas").isEmpty();
        assertThat(exitos.get()).isEqualTo(1);
        assertThat(agotados.get()).isEqualTo(HILOS - 1);
        assertThat(cupoDisponible(cursoId)).isZero();
    }

    @Test
    @DisplayName("liberar devuelve el cupo y no pasa de la capacidad")
    void liberarCupo() {
        Long cursoId = crearCurso(2);
        cursoService.reservarCupo(cursoId);
        assertThat(cupoDisponible(cursoId)).isEqualTo(1);

        CursoResponse liberado = cursoService.liberarCupo(cursoId);
        assertThat(liberado.cupoDisponible()).isEqualTo(2);

        assertThatThrownBy(() -> cursoService.liberarCupo(cursoId))
                .isInstanceOf(CupoAgotadoException.class)
                .extracting("errorType").isEqualTo("CupoYaCompleto");
        assertThat(cupoDisponible(cursoId)).isEqualTo(2);
    }

    @Test
    @DisplayName("PUT que cambia el cupo ajusta cupo_disponible; si no alcanza, 422 y no cambia nada")
    void putAjustaCupo() {
        Long cursoId = crearCurso(5);
        cursoService.reservarCupo(cursoId);
        cursoService.reservarCupo(cursoId);
        cursoService.reservarCupo(cursoId); // 3 reservados, 2 disponibles

        CursoResponse r = cursoService.update(cursoId, put(4, "Martes 08:00-10:00"));
        assertThat(r.cupo()).isEqualTo(4);
        assertThat(r.cupoDisponible()).isEqualTo(1);
        assertThat(r.horario()).isEqualTo("Martes 08:00-10:00");

        // PUT sin cambio de cupo: no toca cupo_disponible
        r = cursoService.update(cursoId, put(4, "Jueves 08:00-10:00"));
        assertThat(r.cupoDisponible()).isEqualTo(1);

        // Bajar a 2 con 3 reservados dejaría -1: se rechaza y se revierte todo el PUT
        assertThatThrownBy(() -> cursoService.update(cursoId, put(2, "Viernes 08:00-10:00")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("errorType").isEqualTo("CupoMenorQueInscritos");
        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT cupo, cupo_disponible, horario FROM cursos WHERE id = ?", cursoId);
        assertThat(fila).containsEntry("cupo", 4)
                .containsEntry("cupo_disponible", 1)
                .containsEntry("horario", "Jueves 08:00-10:00");
    }

    @Test
    @DisplayName("DELETE deja la fila en INACTIVO y ya no acepta reservas")
    void deleteInactiva() {
        Long cursoId = crearCurso(3);

        cursoService.delete(cursoId);

        assertThat(jdbc.queryForObject("SELECT estado FROM cursos WHERE id = ?", String.class, cursoId))
                .isEqualTo("INACTIVO");
        assertThatThrownBy(() -> cursoService.reservarCupo(cursoId))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting("errorType").isEqualTo("CursoInactivo");
        assertThat(cupoDisponible(cursoId)).isEqualTo(3);
    }

    private Long crearCurso(int cupo) {
        Long id = jdbc.queryForObject("""
                INSERT INTO cursos (materia_id, docente_id, horario, periodo, cupo, cupo_disponible,
                                    aula, modalidad, estado)
                VALUES (?, ?, 'Lunes 08:00-10:00', '2099-1', ?, ?, 'IT', 'PRESENCIAL', 'ACTIVO')
                RETURNING id
                """, Long.class, materiaId, docenteId, cupo, cupo);
        cursosCreados.add(id);
        return id;
    }

    private CursoUpdateRequest put(int cupo, String horario) {
        return new CursoUpdateRequest(materiaId, docenteId, horario, "2099-1", cupo, "IT",
                Modalidad.PRESENCIAL, EstadoCurso.ACTIVO);
    }

    private int cupoDisponible(Long cursoId) {
        return jdbc.queryForObject("SELECT cupo_disponible FROM cursos WHERE id = ?", Integer.class, cursoId);
    }

    private static String envOr(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
