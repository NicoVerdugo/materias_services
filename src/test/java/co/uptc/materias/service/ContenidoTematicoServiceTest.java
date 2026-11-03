package co.uptc.materias.service;

import co.uptc.materias.dto.response.ContenidoTematicoResponse;
import co.uptc.materias.entity.ContenidoTematico;
import co.uptc.materias.entity.Materia;
import co.uptc.materias.exception.ArchivoDemasiadoGrandeException;
import co.uptc.materias.exception.ArchivoInvalidoException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.repository.ContenidoTematicoRepository;
import co.uptc.materias.repository.MateriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContenidoTematicoService")
class ContenidoTematicoServiceTest {

    private static final byte[] PDF = "%PDF-1.7\n% contenido de prueba\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private ContenidoTematicoRepository contenidoRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private AlmacenamientoS3Service almacenamiento;

    @InjectMocks
    private ContenidoTematicoService contenidoService;

    private static MockMultipartFile archivo(String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", "programa.pdf", tipo, contenido);
    }

    private Materia materia() {
        return new Materia(1L, "Sistemas Distribuidos", "Descripción", 4, "Ingeniería de Sistemas");
    }

    @Nested
    @DisplayName("subir - validaciones")
    class Validaciones {

        @Test
        @DisplayName("debe rechazar un archivo vacío o ausente")
        void shouldRejectEmpty() {
            assertThatThrownBy(() -> contenidoService.subir(1L, archivo("application/pdf", new byte[0])))
                    .isInstanceOf(ArchivoInvalidoException.class);
            assertThatThrownBy(() -> contenidoService.subir(1L, null))
                    .isInstanceOf(ArchivoInvalidoException.class);
            verifyNoInteractions(almacenamiento);
        }

        @Test
        @DisplayName("debe rechazar un tipo MIME distinto de PDF")
        void shouldRejectNonPdfMime() {
            assertThatThrownBy(() -> contenidoService.subir(1L, archivo("image/png", PDF)))
                    .isInstanceOf(ArchivoInvalidoException.class)
                    .hasMessageContaining("PDF");
            verifyNoInteractions(almacenamiento);
        }

        @Test
        @DisplayName("debe rechazar un archivo declarado como PDF cuyo contenido no lo es")
        void shouldRejectFakePdf() {
            byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            assertThatThrownBy(() -> contenidoService.subir(1L, archivo("application/pdf", png)))
                    .isInstanceOf(ArchivoInvalidoException.class)
                    .hasMessageContaining("no corresponde a un PDF");
            verifyNoInteractions(almacenamiento);
        }

        @Test
        @DisplayName("debe rechazar un PDF de más de 10 MB")
        void shouldRejectTooLarge() {
            byte[] grande = Arrays.copyOf(PDF, (int) ContenidoTematicoService.TAMANO_MAXIMO_BYTES + 1);
            assertThatThrownBy(() -> contenidoService.subir(1L, archivo("application/pdf", grande)))
                    .isInstanceOf(ArchivoDemasiadoGrandeException.class);
            verifyNoInteractions(almacenamiento);
        }

        @Test
        @DisplayName("debe aceptar un PDF de exactamente 10 MB")
        void shouldAcceptExactlyMaxSize() {
            byte[] limite = Arrays.copyOf(PDF, (int) ContenidoTematicoService.TAMANO_MAXIMO_BYTES);
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia()));
            when(contenidoRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            ContenidoTematicoResponse response = contenidoService.subir(1L, archivo("application/pdf", limite));

            assertThat(response.tamanoBytes()).isEqualTo(ContenidoTematicoService.TAMANO_MAXIMO_BYTES);
        }

        @Test
        @DisplayName("debe lanzar ResourceNotFoundException si la materia no existe, sin subir a S3")
        void shouldThrowWhenMateriaNotFound() {
            when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contenidoService.subir(99L, archivo("application/pdf", PDF)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(almacenamiento);
        }
    }

    @Nested
    @DisplayName("subir - S3 y metadatos")
    class Subida {

        @Test
        @DisplayName("debe subir a S3 y guardar solo los metadatos")
        void shouldUploadAndSaveMetadata() {
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia()));
            when(contenidoRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            when(almacenamiento.urlDescarga(anyString(), eq("programa.pdf"))).thenReturn("https://bucket.s3.amazonaws.com/firmada");

            ContenidoTematicoResponse response = contenidoService.subir(1L, archivo("application/pdf", PDF));

            ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
            verify(almacenamiento).subir(clave.capture(), eq(PDF), eq("application/pdf"));
            assertThat(clave.getValue()).matches("materias/1/contenido-tematico/[0-9a-f-]{36}\\.pdf");

            ArgumentCaptor<ContenidoTematico> guardado = ArgumentCaptor.forClass(ContenidoTematico.class);
            verify(contenidoRepository).saveAndFlush(guardado.capture());
            assertThat(guardado.getValue().getClaveS3()).isEqualTo(clave.getValue());
            assertThat(guardado.getValue().getNombreOriginal()).isEqualTo("programa.pdf");
            assertThat(guardado.getValue().getTipoMime()).isEqualTo("application/pdf");
            assertThat(guardado.getValue().getTamanoBytes()).isEqualTo(PDF.length);

            assertThat(response.materiaId()).isEqualTo(1L);
            assertThat(response.url()).isEqualTo("https://bucket.s3.amazonaws.com/firmada");
        }

        @Test
        @DisplayName("debe eliminar el objeto de S3 si falla el guardado de metadatos")
        void shouldDeleteObjectWhenSaveFails() {
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia()));
            when(contenidoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("fallo"));

            assertThatThrownBy(() -> contenidoService.subir(1L, archivo("application/pdf", PDF)))
                    .isInstanceOf(DataIntegrityViolationException.class);

            ArgumentCaptor<String> subida = ArgumentCaptor.forClass(String.class);
            verify(almacenamiento).subir(subida.capture(), any(), any());
            verify(almacenamiento).eliminar(subida.getValue());
        }
    }
}
