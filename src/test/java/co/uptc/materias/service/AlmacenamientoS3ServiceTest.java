package co.uptc.materias.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlmacenamientoS3Service")
class AlmacenamientoS3ServiceTest {

    @Test
    @DisplayName("Content-Disposition fuerza la descarga con el nombre original")
    void shouldBuildAttachmentDisposition() {
        assertThat(AlmacenamientoS3Service.contentDisposition("programa.pdf"))
                .isEqualTo("attachment; filename=\"programa.pdf\"; filename*=UTF-8''programa.pdf");
    }

    @Test
    @DisplayName("Tildes, espacios y comillas quedan codificados en filename* y reemplazados en filename")
    void shouldEncodeNonAsciiNames() {
        assertThat(AlmacenamientoS3Service.contentDisposition("Programación \"temática\".pdf"))
                .isEqualTo("attachment; filename=\"Programaci_n _tem_tica_.pdf\"; "
                        + "filename*=UTF-8''Programaci%C3%B3n%20%22tem%C3%A1tica%22.pdf");
    }
}
