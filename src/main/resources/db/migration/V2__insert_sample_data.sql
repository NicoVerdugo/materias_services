-- =============================================
-- V2: Datos de prueba para demostración
-- Datos ficticios con coherencia académica
-- =============================================

-- === MATERIAS ===
INSERT INTO materias (nombre, descripcion, creditos, programa) VALUES
('Sistemas Distribuidos', 'Estudio de sistemas de cómputo distribuido, paradigmas de comunicación, tolerancia a fallos y consistencia', 4, 'Ingeniería de Sistemas y Computación'),
('Bases de Datos', 'Diseño, implementación y administración de sistemas de bases de datos relacionales y no relacionales', 4, 'Ingeniería de Sistemas y Computación'),
('Ingeniería de Software', 'Metodologías, procesos y buenas prácticas para el desarrollo de software de calidad', 3, 'Ingeniería de Sistemas y Computación'),
('Redes de Computadores', 'Fundamentos de redes, protocolos de comunicación, arquitecturas y seguridad en redes', 3, 'Ingeniería de Sistemas y Computación'),
('Arquitectura de Software', 'Patrones arquitectónicos, diseño de sistemas escalables y mantenibles', 3, 'Ingeniería de Sistemas y Computación'),
('Inteligencia Artificial', 'Fundamentos de IA, aprendizaje automático, redes neuronales y procesamiento de lenguaje natural', 4, 'Ingeniería de Sistemas y Computación'),
('Cálculo Diferencial', 'Límites, derivadas, aplicaciones de la derivada e introducción a la integral', 4, 'Ingeniería de Sistemas y Computación'),
('Física Mecánica', 'Cinemática, dinámica, trabajo, energía y momento lineal', 4, 'Ingeniería de Sistemas y Computación');

-- === DOCENTES ===
INSERT INTO docentes (nombres, apellidos, correo_institucional, especialidad, activo) VALUES
('Carlos Andrés', 'Ramírez López', 'carlos.ramirez@uptc.edu.co', 'Sistemas Distribuidos y Cloud Computing', true),
('María Fernanda', 'García Torres', 'maria.garcia@uptc.edu.co', 'Bases de Datos y Big Data', true),
('Luis Eduardo', 'Martínez Pérez', 'luis.martinez@uptc.edu.co', 'Ingeniería de Software', true),
('Ana Patricia', 'Rodríguez Díaz', 'ana.rodriguez@uptc.edu.co', 'Redes y Seguridad Informática', true),
('Jorge Enrique', 'Hernández Vargas', 'jorge.hernandez@uptc.edu.co', 'Arquitectura de Software y Microservicios', true),
('Diana Carolina', 'López Mendoza', 'diana.lopez@uptc.edu.co', 'Inteligencia Artificial y Machine Learning', true),
('Roberto Carlos', 'Suárez Pineda', 'roberto.suarez@uptc.edu.co', 'Matemáticas Aplicadas', false);

-- === CURSOS ===
INSERT INTO cursos (materia_id, docente_id, horario, periodo, cupo, aula, modalidad, estado) VALUES
-- Sistemas Distribuidos
(1, 1, 'Lunes y Miércoles 14:00-16:00', '2026-2', 30, 'Laboratorio 204', 'PRESENCIAL', 'ACTIVO'),
-- Bases de Datos
(2, 2, 'Martes y Jueves 08:00-10:00', '2026-2', 35, 'Laboratorio 201', 'PRESENCIAL', 'ACTIVO'),
-- Ingeniería de Software
(3, 3, 'Lunes y Miércoles 10:00-12:00', '2026-2', 40, 'Aula 305', 'HIBRIDA', 'ACTIVO'),
-- Redes de Computadores
(4, 4, 'Martes y Jueves 14:00-16:00', '2026-2', 30, 'Laboratorio 203', 'PRESENCIAL', 'ACTIVO'),
-- Arquitectura de Software
(5, 5, 'Viernes 08:00-12:00', '2026-2', 25, 'Aula 401', 'VIRTUAL', 'ACTIVO'),
-- Inteligencia Artificial
(6, 6, 'Lunes y Miércoles 16:00-18:00', '2026-2', 30, 'Laboratorio 205', 'PRESENCIAL', 'ACTIVO'),
-- Sistemas Distribuidos (periodo anterior)
(1, 1, 'Martes y Jueves 10:00-12:00', '2026-1', 30, 'Laboratorio 204', 'PRESENCIAL', 'INACTIVO'),
-- Bases de Datos (periodo anterior)
(2, 2, 'Lunes y Miércoles 08:00-10:00', '2026-1', 35, 'Laboratorio 201', 'PRESENCIAL', 'INACTIVO'),
-- Cálculo Diferencial
(7, 7, 'Martes y Jueves 06:00-08:00', '2026-2', 45, 'Aula 102', 'PRESENCIAL', 'CANCELADO'),
-- Bases de Datos (virtual)
(2, 6, 'Sábados 08:00-12:00', '2026-2', 50, 'Virtual', 'VIRTUAL', 'ACTIVO');
