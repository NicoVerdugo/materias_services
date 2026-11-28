package co.uptc.materias.datalake;

import co.uptc.materias.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Registra una línea por petición para el datalake (contrato común a todos los módulos).
 * <p>
 * Va primero en la cadena de filtros ({@link Ordered#HIGHEST_PRECEDENCE}) para que la
 * latencia y el estado cubran todo el procesamiento. Los cuerpos se leen de
 * {@link ContentCachingRequestWrapper} / {@link ContentCachingResponseWrapper}; al
 * terminar se copia el cuerpo a la respuesta real, se envía al cliente y solo
 * entonces se arma y escribe la línea, envuelta en try/catch: un fallo al registrar
 * nunca cambia ni retrasa la respuesta.
 * <p>
 * Siempre devuelve el encabezado {@code x-trace-id}, también en las rutas que no se
 * registran (health, actuator y documentación).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatalakeLogFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "x-trace-id";
    static final int LIMITE_CUERPO = 16 * 1024;

    /** Formato aceptado para un x-trace-id recibido. */
    static final Pattern TRACE_VALIDO = Pattern.compile("^[A-Za-z0-9-]{1,128}$");

    static final String CUERPO_NO_JSON = "<no-json>";
    static final String CUERPO_DEMASIADO_GRANDE = "<demasiado-grande>";
    static final String CUERPO_OMITIDO = "<omitido>";

    /** Rutas cuyo cuerpo nunca se registra (credenciales). */
    private static final List<String> RUTAS_SIN_CUERPO = List.of("/api/auth/login", "/api/auth/registro");

    /** Rutas que no se registran. */
    private static final List<String> PREFIJOS_EXCLUIDOS = List.of(
            "/health", "/actuator", "/swagger-ui", "/v3/api-docs", "/api-docs");

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /** Mapper propio: no depende de la configuración de Jackson de la app (p. ej. NON_NULL). */
    private static final ObjectMapper JSON = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT);

    private final DatalakeLogWriter writer;

    /**
     * @param logDir   variable LOG_DIR (vacía: solo salida estándar)
     * @param instance variable HOSTNAME (vacía: "local")
     */
    @Autowired
    public DatalakeLogFilter(@Value("${LOG_DIR:}") String logDir,
                             @Value("${HOSTNAME:local}") String instance) {
        this(new DatalakeLogWriter(logDir, instance));
    }

    /** Para pruebas: permite capturar las líneas o simular fallos al escribir. */
    DatalakeLogFilter(DatalakeLogWriter writer) {
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Instant llegada = Instant.now();
        long inicio = System.nanoTime();
        String traceId = traceId(request);
        response.setHeader(TRACE_HEADER, traceId);

        if (excluida(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, LIMITE_CUERPO + 1);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
        boolean excepcion = false;
        try {
            chain.doFilter(req, res);
        } catch (IOException | ServletException | RuntimeException | Error e) {
            excepcion = true;
            throw e;
        } finally {
            try {
                res.copyBodyToResponse();
                if (!excepcion) {
                    enviarAlCliente(response);
                }
            } finally {
                registrar(req, res, traceId, llegada, inicio, excepcion);
            }
        }
    }

    /**
     * Arma y escribe la línea. Nunca lanza: cualquier fallo se descarta.
     */
    private void registrar(ContentCachingRequestWrapper req, ContentCachingResponseWrapper res,
                           String traceId, Instant llegada, long inicio, boolean excepcion) {
        try {
            long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;
            // Si algo escapó de la cadena, el contenedor responderá 500
            int status = excepcion ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : res.getStatus();

            Map<String, Object> linea = new LinkedHashMap<>();
            linea.put("ts", TS.format(llegada));
            linea.put("service", DatalakeLogWriter.SERVICE);
            linea.put("instance", writer.instance());
            linea.put("trace_id", traceId);
            linea.put("method", req.getMethod());
            linea.put("path", req.getRequestURI());
            linea.put("query", vacioANull(req.getQueryString()));
            linea.put("status", status);
            linea.put("latency_ms", latenciaMs);
            linea.put("user_id", vacioANull(req.getHeader("x-user-id")));
            linea.put("role", vacioANull(req.getHeader("x-user-role")));
            linea.put("idempotency_key", vacioANull(req.getHeader("idempotency-key")));
            linea.put("error_type", errorType(req));
            linea.put("request_body", cuerpo(req));

            writer.escribir(JSON.writeValueAsString(linea));
        } catch (Throwable ignorada) {
            // registrar nunca debe romper la petición
        }
    }

    /**
     * Valor de {@code request_body} según el contrato.
     */
    private Object cuerpo(ContentCachingRequestWrapper req) {
        if (RUTAS_SIN_CUERPO.contains(req.getRequestURI())) {
            return CUERPO_OMITIDO;
        }
        long declarado = req.getContentLengthLong();
        String contentType = req.getContentType();
        boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
        boolean tieneCuerpo = multipart || declarado > 0 || req.getHeader("Transfer-Encoding") != null
                || req.getContentAsByteArray().length > 0;
        if (!tieneCuerpo) {
            return null;
        }
        if (!esJson(contentType)) {
            // multipart (PDF del contenido temático), binario, formularios...
            return CUERPO_NO_JSON;
        }
        if (declarado > LIMITE_CUERPO) {
            return CUERPO_DEMASIADO_GRANDE;
        }
        byte[] bytes = cuerpoJson(req);
        if (bytes.length == 0) {
            return null;
        }
        if (bytes.length > LIMITE_CUERPO) {
            return CUERPO_DEMASIADO_GRANDE;
        }
        try {
            return JSON.readTree(bytes);
        } catch (IOException noEsJsonValido) {
            return CUERPO_NO_JSON;
        }
    }

    /**
     * Bytes del cuerpo JSON. Si la aplicación no llegó a leerlo (ruta inexistente,
     * error antes del controlador) se termina de leer aquí, hasta el límite.
     */
    private byte[] cuerpoJson(ContentCachingRequestWrapper req) {
        byte[] leidos = req.getContentAsByteArray();
        if (leidos.length <= LIMITE_CUERPO) {
            try {
                InputStream in = req.getInputStream(); // cachea lo que se lea
                in.readNBytes(LIMITE_CUERPO + 1 - leidos.length);
                leidos = req.getContentAsByteArray();
            } catch (Exception sinMasDatos) {
                // stream cerrado o ya consumido por otra vía: se usa lo que haya
            }
        }
        return leidos;
    }

    /**
     * Envía la respuesta ya completa (copyBodyToResponse fija Content-Length) antes de
     * registrar, para que escribir la línea no retrase al cliente.
     */
    private static void enviarAlCliente(HttpServletResponse response) {
        try {
            response.flushBuffer();
        } catch (IOException clienteDesconectado) {
            // el contenedor lo gestiona igual que sin este filtro
        }
    }

    private static Object errorType(HttpServletRequest req) {
        Object valor = req.getAttribute(GlobalExceptionHandler.ERROR_TYPE_ATTRIBUTE);
        return valor == null ? null : valor.toString();
    }

    private static boolean esJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType tipo = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.isCompatibleWith(tipo)
                    || tipo.getSubtype().endsWith("+json");
        } catch (Exception tipoInvalido) {
            return false;
        }
    }

    static boolean excluida(String path) {
        for (String prefijo : PREFIJOS_EXCLUIDOS) {
            if (path.equals(prefijo) || path.startsWith(prefijo + "/") || path.startsWith(prefijo + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reutiliza el x-trace-id recibido solo si cumple {@link #TRACE_VALIDO}; si falta o
     * trae otros caracteres (espacios, comillas, saltos de línea...) o pasa de 128, se
     * genera uno nuevo. Así el valor nunca rompe el log ni el encabezado de respuesta.
     */
    static String traceId(HttpServletRequest request) {
        String recibido = request.getHeader(TRACE_HEADER);
        if (recibido != null && TRACE_VALIDO.matcher(recibido).matches()) {
            return recibido;
        }
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
