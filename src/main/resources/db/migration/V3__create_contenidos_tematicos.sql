-- =============================================================
-- V3: Contenido temático de las materias (archivos PDF en AWS S3)
-- Solo se guardan los metadatos; el archivo vive en el bucket.
-- =============================================================

CREATE TABLE contenidos_tematicos (
    id              BIGSERIAL       PRIMARY KEY,
    materia_id      BIGINT          NOT NULL REFERENCES materias(id),
    clave_s3        VARCHAR(512)    NOT NULL UNIQUE,
    nombre_original VARCHAR(255)    NOT NULL,
    tipo_mime       VARCHAR(100)    NOT NULL CHECK (tipo_mime = 'application/pdf'),
    tamano_bytes    BIGINT          NOT NULL CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760)
);

CREATE INDEX idx_contenidos_tematicos_materia_id ON contenidos_tematicos(materia_id);
