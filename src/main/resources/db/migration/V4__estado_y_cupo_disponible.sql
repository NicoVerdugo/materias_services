-- =============================================================
-- V4: Cupo disponible para la consistencia sin FK con Inscripciones
--
-- cupo            = capacidad del curso (se mantiene CHECK cupo > 0)
-- cupo_disponible = puestos libres; lo descuenta y devuelve el módulo
--                   Inscripciones por HTTP (reservar-cupo / liberar-cupo)
--                   con sentencias UPDATE condicionales y atómicas.
--
-- Los cursos ya no se borran: DELETE pasa el estado a INACTIVO,
-- usando los valores que ya permite el CHECK de V1 (no requiere DDL).
-- =============================================================

ALTER TABLE cursos ADD COLUMN cupo_disponible INTEGER;

-- Cursos existentes: aún sin inscripciones registradas en este módulo
UPDATE cursos SET cupo_disponible = cupo;

ALTER TABLE cursos ALTER COLUMN cupo_disponible SET NOT NULL;

ALTER TABLE cursos
    ADD CONSTRAINT chk_cursos_cupo_disponible
    CHECK (cupo_disponible >= 0 AND cupo_disponible <= cupo);
