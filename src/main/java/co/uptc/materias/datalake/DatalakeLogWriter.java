package co.uptc.materias.datalake;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Destino de las líneas del datalake.
 * <ul>
 *   <li>Siempre: salida estándar, con el prefijo {@value #PREFIJO}.</li>
 *   <li>Si está definida la variable {@code LOG_DIR}: la misma línea, sin prefijo, se
 *       agrega a {@code LOG_DIR/materias-<instance>-AAAAMMDD-HH.jsonl} (hora UTC).</li>
 * </ul>
 * Se usa {@code System.out} y no el logger de Spring para que la línea no lleve el
 * patrón de fecha y nivel delante. Ningún método lanza excepciones.
 */
public class DatalakeLogWriter {

    public static final String PREFIJO = "DLK ";
    public static final String SERVICE = "materias";

    private static final DateTimeFormatter HORA_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd-HH").withZone(ZoneOffset.UTC);

    private final Path logDir;
    private final String instance;

    public DatalakeLogWriter(String logDir, String instance) {
        this.logDir = logDir == null || logDir.isBlank() ? null : Path.of(logDir);
        this.instance = instance == null || instance.isBlank() ? "local" : instance;
    }

    /** Valor del campo {@code instance}: la variable HOSTNAME o "local". */
    public String instance() {
        return instance;
    }

    /**
     * Escribe una línea JSON compacta (sin saltos de línea). Los fallos se ignoran
     * a propósito: registrar nunca debe afectar a la petición.
     */
    public void escribir(String json) {
        try {
            salida().println(PREFIJO + json);
        } catch (RuntimeException ignorada) {
            // stdout no disponible: se pierde la línea, la petición sigue
        }
        if (logDir != null) {
            escribirArchivo(json, Instant.now());
        }
    }

    /** Ruta del archivo de la hora dada, por ejemplo materias-local-20261127-14.jsonl. */
    Path archivoPara(Instant momento) {
        return logDir.resolve(SERVICE + "-" + instance + "-" + HORA_ARCHIVO.format(momento) + ".jsonl");
    }

    /** Sincronizado para que dos hilos no intercalen bytes de líneas distintas. */
    private synchronized void escribirArchivo(String json, Instant momento) {
        try {
            Files.createDirectories(logDir);
            Files.writeString(archivoPara(momento), json + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignorada) {
            // disco lleno, permisos, etc.: la línea ya salió por stdout
        }
    }

    /** Punto de extensión para pruebas. */
    PrintStream salida() {
        return System.out;
    }
}
