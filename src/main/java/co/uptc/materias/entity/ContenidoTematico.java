package co.uptc.materias.entity;

import jakarta.persistence.*;

/**
 * Metadatos de un archivo de contenido temático (PDF) asociado a una materia.
 * <p>
 * El archivo se almacena en AWS S3; aquí solo se guarda su clave en el bucket,
 * el nombre original, el tipo MIME y el tamaño en bytes.
 */
@Entity
@Table(name = "contenidos_tematicos")
public class ContenidoTematico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(name = "clave_s3", nullable = false, unique = true, length = 512)
    private String claveS3;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    public ContenidoTematico() {
    }

    public ContenidoTematico(Materia materia, String claveS3, String nombreOriginal, String tipoMime, Long tamanoBytes) {
        this.materia = materia;
        this.claveS3 = claveS3;
        this.nombreOriginal = nombreOriginal;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
    }

    // --- Getters y Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public String getClaveS3() {
        return claveS3;
    }

    public void setClaveS3(String claveS3) {
        this.claveS3 = claveS3;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }
}
