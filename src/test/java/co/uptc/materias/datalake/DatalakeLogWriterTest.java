package co.uptc.materias.datalake;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("DatalakeLogWriter")
class DatalakeLogWriterTest {

    private static final String LINEA = "{\"ts\":\"2026-11-27T14:03:22.123Z\",\"service\":\"materias\"}";

    @TempDir
    Path tmp;

    @Test
    @DisplayName("imprime la línea con el prefijo DLK por la salida estándar")
    void imprimeConPrefijo() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        DatalakeLogWriter writer = conSalida("", salida);

        writer.escribir(LINEA);

        assertThat(salida.toString(StandardCharsets.UTF_8)).isEqualTo("DLK " + LINEA + System.lineSeparator());
    }

    @Test
    @DisplayName("con LOG_DIR agrega la línea sin prefijo a <service>-<instance>-AAAAMMDD-HH.jsonl y crea la carpeta")
    void escribeArchivoPorHora() throws Exception {
        Path dir = tmp.resolve("no/existe/aun");
        DatalakeLogWriter writer = conSalida(dir.toString(), new ByteArrayOutputStream());

        Instant antes = Instant.now();
        writer.escribir(LINEA);
        writer.escribir(LINEA);

        Path archivo = writer.archivoPara(antes);
        assertThat(archivo.getFileName().toString()).matches("materias-host-1-\\d{8}-\\d{2}\\.jsonl");
        assertThat(Files.readAllLines(archivo)).containsExactly(LINEA, LINEA);
    }

    @Test
    @DisplayName("sin HOSTNAME la instancia es local")
    void instanciaLocal() {
        assertThat(new DatalakeLogWriter("", "").instance()).isEqualTo("local");
        assertThat(new DatalakeLogWriter("", null).instance()).isEqualTo("local");
    }

    @Test
    @DisplayName("si LOG_DIR no se puede usar, no lanza y la línea sigue saliendo por stdout")
    void logDirInvalidoNoLanza() throws Exception {
        Path archivoComoCarpeta = Files.writeString(tmp.resolve("soy-un-archivo"), "x");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        DatalakeLogWriter writer = conSalida(archivoComoCarpeta.toString(), salida);

        assertThatCode(() -> writer.escribir(LINEA)).doesNotThrowAnyException();
        assertThat(salida.toString(StandardCharsets.UTF_8)).startsWith("DLK ");
    }

    private static DatalakeLogWriter conSalida(String logDir, ByteArrayOutputStream destino) {
        PrintStream ps = new PrintStream(destino, true, StandardCharsets.UTF_8);
        return new DatalakeLogWriter(logDir, "host-1") {
            @Override
            PrintStream salida() {
                return ps;
            }
        };
    }
}
