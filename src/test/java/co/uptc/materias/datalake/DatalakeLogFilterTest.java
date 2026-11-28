package co.uptc.materias.datalake;

import co.uptc.materias.controller.ContenidoTematicoController;
import co.uptc.materias.controller.CursoController;
import co.uptc.materias.dto.response.ContenidoTematicoResponse;
import co.uptc.materias.dto.response.CursoResponse;
import co.uptc.materias.exception.CupoAgotadoException;
import co.uptc.materias.service.ContenidoTematicoService;
import co.uptc.materias.service.CursoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Filtro del datalake sobre los controladores y el GlobalExceptionHandler reales.
 * El MockMvc se arma a mano con un writer que captura las líneas en memoria.
 */
@WebMvcTest(controllers = {CursoController.class, ContenidoTematicoController.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DatalakeLogFilter")
class DatalakeLogFilterTest {

    private static final List<String> CAMPOS = List.of(
            "ts", "service", "instance", "trace_id", "method", "path", "query", "status",
            "latency_ms", "user_id", "role", "idempotency_key", "error_type", "request_body");

    private static final String CURSO_JSON = """
            {"materiaId":1,"docenteId":1,"horario":"Lunes 08:00-10:00","periodo":"2026-2",
             "cupo":30,"aula":"Aula 101","modalidad":"PRESENCIAL","estado":"ACTIVO"}
            """;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private CursoService cursoService;

    @MockBean
    private ContenidoTematicoService contenidoService;

    private final ObjectMapper json = new ObjectMapper();
    private final List<String> lineas = Collections.synchronizedList(new ArrayList<>());
    private MockMvc mockMvc;

    /** Writer que guarda las líneas en vez de imprimirlas. */
    private final DatalakeLogWriter capturador = new DatalakeLogWriter("", "test-host") {
        @Override
        public void escribir(String linea) {
            lineas.add(linea);
        }
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DatalakeLogFilter(capturador))
                .build();
    }

    @Test
    @DisplayName("una línea por petición con todos los campos del contrato")
    void unaLineaConTodosLosCampos() throws Exception {
        when(cursoService.findById(1L)).thenReturn(curso());

        mockMvc.perform(get("/api/materias/cursos/1").queryParam("ver", "completo")
                        .header("x-trace-id", "abc123")
                        .header("x-user-id", "42")
                        .header("x-user-role", "ESTUDIANTE")
                        .header("idempotency-key", "idem-1"))
                .andExpect(status().isOk());

        assertThat(lineas).hasSize(1);
        JsonNode l = json.readTree(lineas.getFirst());
        assertThat(nombres(l)).containsExactlyElementsOf(CAMPOS);
        assertThat(l.get("ts").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
        assertThat(l.get("service").asText()).isEqualTo("materias");
        assertThat(l.get("instance").asText()).isEqualTo("test-host");
        assertThat(l.get("trace_id").asText()).isEqualTo("abc123");
        assertThat(l.get("method").asText()).isEqualTo("GET");
        assertThat(l.get("path").asText()).isEqualTo("/api/materias/cursos/1");
        assertThat(l.get("query").asText()).isEqualTo("ver=completo");
        assertThat(l.get("status").asInt()).isEqualTo(200);
        assertThat(l.get("latency_ms").isIntegralNumber()).isTrue();
        assertThat(l.get("latency_ms").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(l.get("user_id").asText()).isEqualTo("42");
        assertThat(l.get("role").asText()).isEqualTo("ESTUDIANTE");
        assertThat(l.get("idempotency_key").asText()).isEqualTo("idem-1");
        assertThat(l.get("error_type").isNull()).isTrue();
        assertThat(l.get("request_body").isNull()).isTrue();
        assertThat(lineas.getFirst()).doesNotContain("\n");
    }

    @Test
    @DisplayName("sin datos opcionales: los campos están presentes con null")
    void camposOpcionalesEnNull() throws Exception {
        when(cursoService.findById(1L)).thenReturn(curso());

        mockMvc.perform(get("/api/materias/cursos/1")).andExpect(status().isOk());

        JsonNode l = json.readTree(lineas.getFirst());
        assertThat(nombres(l)).containsExactlyElementsOf(CAMPOS);
        for (String campo : List.of("query", "user_id", "role", "idempotency_key", "error_type", "request_body")) {
            assertThat(l.get(campo).isNull()).as(campo).isTrue();
        }
    }

    @Test
    @DisplayName("reutiliza el x-trace-id recibido y lo devuelve")
    void reutilizaTraceId() throws Exception {
        when(cursoService.findById(1L)).thenReturn(curso());

        mockMvc.perform(get("/api/materias/cursos/1").header("x-trace-id", "0123456789abcdef"))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", "0123456789abcdef"));

        assertThat(json.readTree(lineas.getFirst()).get("trace_id").asText()).isEqualTo("0123456789abcdef");
    }

    @Test
    @DisplayName("si no llega x-trace-id genera 16 hex, lo devuelve y lo registra")
    void generaTraceId() throws Exception {
        when(cursoService.findById(1L)).thenReturn(curso());

        MvcResult r = mockMvc.perform(get("/api/materias/cursos/1"))
                .andExpect(status().isOk())
                .andReturn();

        String devuelto = r.getResponse().getHeader("x-trace-id");
        assertThat(devuelto).matches("[0-9a-f]{16}");
        assertThat(json.readTree(lineas.getFirst()).get("trace_id").asText()).isEqualTo(devuelto);
    }

    @Test
    @DisplayName("POST JSON: request_body es el objeto JSON")
    void cuerpoJson() throws Exception {
        when(cursoService.create(any())).thenReturn(curso());

        mockMvc.perform(post("/api/materias/cursos").contentType(MediaType.APPLICATION_JSON).content(CURSO_JSON))
                .andExpect(status().isCreated());

        JsonNode cuerpo = json.readTree(lineas.getFirst()).get("request_body");
        assertThat(cuerpo.isObject()).isTrue();
        assertThat(cuerpo.get("aula").asText()).isEqualTo("Aula 101");
        assertThat(cuerpo.get("cupo").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("/api/auth/login y /api/auth/registro nunca guardan el cuerpo")
    void loginOmiteCuerpo() throws Exception {
        String credenciales = "{\"correo\":\"a@b.co\",\"password\":\"secreta123\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credenciales));
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(credenciales));

        assertThat(lineas).hasSize(2);
        for (String linea : lineas) {
            assertThat(json.readTree(linea).get("request_body").asText()).isEqualTo("<omitido>");
            assertThat(linea).doesNotContain("secreta123");
        }
    }

    @Test
    @DisplayName("multipart (PDF): request_body = <no-json>")
    void multipartNoJson() throws Exception {
        when(contenidoService.subir(anyLong(), any())).thenReturn(new ContenidoTematicoResponse(
                1L, 1L, "materias/1/contenido-tematico/x.pdf", "plan.pdf", "application/pdf", 9L, null));
        MockMultipartFile pdf = new MockMultipartFile("archivo", "plan.pdf", "application/pdf",
                "%PDF-1.4\n".getBytes());

        mockMvc.perform(multipart("/api/materias/1/contenido-tematico").file(pdf))
                .andExpect(status().isCreated());

        JsonNode l = json.readTree(lineas.getFirst());
        assertThat(l.get("request_body").asText()).isEqualTo("<no-json>");
        assertThat(l.get("status").asInt()).isEqualTo(201);
    }

    @Test
    @DisplayName("cuerpo JSON de más de 16 KB: <demasiado-grande>")
    void cuerpoDemasiadoGrande() throws Exception {
        String grande = "{\"texto\":\"" + "x".repeat(17 * 1024) + "\"}";

        mockMvc.perform(post("/api/materias/cursos").contentType(MediaType.APPLICATION_JSON).content(grande));

        assertThat(json.readTree(lineas.getFirst()).get("request_body").asText()).isEqualTo("<demasiado-grande>");
    }

    @Test
    @DisplayName("error_type sale con el código de negocio del GlobalExceptionHandler")
    void errorTypeDeNegocio() throws Exception {
        when(cursoService.reservarCupo(7L)).thenThrow(CupoAgotadoException.agotado(7L));

        mockMvc.perform(post("/api/materias/cursos/7/reservar-cupo"))
                .andExpect(status().isConflict());

        JsonNode l = json.readTree(lineas.getFirst());
        assertThat(l.get("status").asInt()).isEqualTo(409);
        assertThat(l.get("error_type").asText()).isEqualTo("CupoAgotado");
    }

    @Test
    @DisplayName("ruta inexistente: 404 con error_type RutaNoExiste y el cuerpo JSON aunque nadie lo leyó")
    void rutaInexistente() throws Exception {
        mockMvc.perform(post("/api/materias/no-existe").contentType(MediaType.APPLICATION_JSON).content("{\"a\":1}"))
                .andExpect(status().isNotFound());

        JsonNode l = json.readTree(lineas.getFirst());
        assertThat(l.get("error_type").asText()).isEqualTo("RutaNoExiste");
        assertThat(l.get("request_body").get("a").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("un error al escribir el log no cambia la respuesta")
    void falloAlEscribirNoCambiaRespuesta() throws Exception {
        when(cursoService.findById(1L)).thenReturn(curso());
        DatalakeLogWriter roto = new DatalakeLogWriter("", "test-host") {
            @Override
            public void escribir(String linea) {
                throw new IllegalStateException("disco lleno");
            }
        };
        MockMvc conWriterRoto = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DatalakeLogFilter(roto))
                .build();

        String esperado = mockMvc.perform(get("/api/materias/cursos/1").header("x-trace-id", "t1"))
                .andReturn().getResponse().getContentAsString();

        conWriterRoto.perform(get("/api/materias/cursos/1").header("x-trace-id", "t1"))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", "t1"))
                .andExpect(content().json(esperado, true))
                .andExpect(jsonPath("$.cupoDisponible").value(18));
    }

    @Test
    @DisplayName("health, actuator y documentación no se registran, pero devuelven x-trace-id")
    void rutasExcluidas() throws Exception {
        for (String ruta : List.of("/health", "/actuator/health", "/v3/api-docs", "/swagger-ui/index.html",
                "/swagger-ui.html")) {
            mockMvc.perform(get(ruta)).andExpect(header().exists("x-trace-id"));
        }
        assertThat(lineas).isEmpty();
    }

    private static List<String> nombres(JsonNode nodo) {
        List<String> campos = new ArrayList<>();
        nodo.fieldNames().forEachRemaining(campos::add);
        return campos;
    }

    private static CursoResponse curso() {
        return new CursoResponse(1L, 1L, "Sistemas Distribuidos", 1L, "Lunes 08:00-10:00",
                "2026-2", 30, 18, "Aula 101", "PRESENCIAL", "ACTIVO");
    }
}
