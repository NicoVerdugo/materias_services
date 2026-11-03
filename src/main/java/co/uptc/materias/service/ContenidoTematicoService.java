package co.uptc.materias.service;

import co.uptc.materias.dto.response.ContenidoTematicoResponse;
import co.uptc.materias.entity.ContenidoTematico;
import co.uptc.materias.entity.Materia;
import co.uptc.materias.exception.ArchivoDemasiadoGrandeException;
import co.uptc.materias.exception.ArchivoInvalidoException;
import co.uptc.materias.exception.ResourceNotFoundException;
import co.uptc.materias.repository.ContenidoTematicoRepository;
import co.uptc.materias.repository.MateriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para subir y consultar el contenido temático (PDF) de una materia.
 * <p>
 * El archivo se envía directamente a AWS S3; en PostgreSQL solo se guardan sus metadatos.
 */
@Service
@Transactional(readOnly = true)
public class ContenidoTematicoService {

    private static final Logger log = LoggerFactory.getLogger(ContenidoTematicoService.class);

    public static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final String TIPO_PDF = "application/pdf";
    // Firma binaria de todo PDF: el tipo MIME lo declara el cliente y no basta
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int LONGITUD_MAXIMA_NOMBRE = 255;

    private final ContenidoTematicoRepository contenidoRepository;
    private final MateriaRepository materiaRepository;
    private final AlmacenamientoS3Service almacenamiento;

    public ContenidoTematicoService(ContenidoTematicoRepository contenidoRepository,
                                    MateriaRepository materiaRepository,
                                    AlmacenamientoS3Service almacenamiento) {
        this.contenidoRepository = contenidoRepository;
        this.materiaRepository = materiaRepository;
        this.almacenamiento = almacenamiento;
    }

    /**
     * Valida el archivo, lo sube a S3 y guarda sus metadatos.
     *
     * @param materiaId identificador de la materia
     * @param archivo   PDF recibido como multipart/form-data
     * @return los metadatos guardados y una URL firmada temporal
     * @throws ArchivoInvalidoException        si falta el archivo o no es un PDF
     * @throws ArchivoDemasiadoGrandeException si supera los 10 MB
     * @throws ResourceNotFoundException       si la materia no existe
     */
    @Transactional
    public ContenidoTematicoResponse subir(Long materiaId, MultipartFile archivo) {
        byte[] contenido = validar(archivo);

        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada con id: " + materiaId));

        String clave = "materias/%d/contenido-tematico/%s.pdf".formatted(materiaId, UUID.randomUUID());
        almacenamiento.subir(clave, contenido, TIPO_PDF);

        ContenidoTematico guardado;
        try {
            guardado = contenidoRepository.saveAndFlush(new ContenidoTematico(
                    materia, clave, nombreOriginal(archivo), TIPO_PDF, (long) contenido.length));
        } catch (RuntimeException ex) {
            // Si no se pudieron guardar los metadatos, el objeto subido queda huérfano: se elimina
            almacenamiento.eliminar(clave);
            throw ex;
        }

        log.info("Contenido temático {} subido para la materia {}", clave, materiaId);
        return toResponse(guardado);
    }

    /**
     * Lista el contenido temático de una materia, con URLs firmadas temporales.
     *
     * @throws ResourceNotFoundException si la materia no existe
     */
    public List<ContenidoTematicoResponse> findByMateria(Long materiaId) {
        if (!materiaRepository.existsById(materiaId)) {
            throw new ResourceNotFoundException("Materia no encontrada con id: " + materiaId);
        }
        return contenidoRepository.findByMateriaIdOrderByIdAsc(materiaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private byte[] validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Debe enviar un archivo PDF en el campo \"archivo\" (multipart/form-data)");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoDemasiadoGrandeException("El archivo supera el tamaño máximo permitido de 10 MB");
        }
        if (!TIPO_PDF.equalsIgnoreCase(archivo.getContentType())) {
            throw new ArchivoInvalidoException("Formato no permitido: el contenido temático debe ser un archivo PDF");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo enviado");
        }
        if (contenido.length < FIRMA_PDF.length
                || !Arrays.equals(contenido, 0, FIRMA_PDF.length, FIRMA_PDF, 0, FIRMA_PDF.length)) {
            throw new ArchivoInvalidoException("El contenido del archivo no corresponde a un PDF");
        }
        return contenido;
    }

    private String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "contenido.pdf";
        }
        return nombre.length() > LONGITUD_MAXIMA_NOMBRE ? nombre.substring(0, LONGITUD_MAXIMA_NOMBRE) : nombre;
    }

    private ContenidoTematicoResponse toResponse(ContenidoTematico contenido) {
        return new ContenidoTematicoResponse(
                contenido.getId(),
                contenido.getMateria().getId(),
                contenido.getClaveS3(),
                contenido.getNombreOriginal(),
                contenido.getTipoMime(),
                contenido.getTamanoBytes(),
                almacenamiento.urlDescarga(contenido.getClaveS3(), contenido.getNombreOriginal())
        );
    }
}
