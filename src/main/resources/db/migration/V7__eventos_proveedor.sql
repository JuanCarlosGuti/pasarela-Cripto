-- V7: eventos crudos del proveedor (HU-010/011).
-- Cada webhook se guarda tal cual llega, ANTES de procesarlo. La unicidad
-- (proveedor, id_externo_evento) es la última línea de defensa de la
-- idempotencia (ADR-004): procesar dos veces el mismo evento es imposible.
-- id_externo_evento es NULL solo en intentos con firma inválida (el índice
-- único de PostgreSQL ignora los NULL: se permiten varios intentos).

CREATE TABLE eventos_proveedor (
    id                UUID         PRIMARY KEY,
    proveedor         VARCHAR(50)  NOT NULL,
    id_externo_evento VARCHAR(100),
    tipo              VARCHAR(50),
    carga_cruda       TEXT         NOT NULL,
    firma_valida      BOOLEAN      NOT NULL,
    procesado         BOOLEAN      NOT NULL,
    nota_revision     VARCHAR(500),
    recibido_en       TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_eventos_proveedor_idempotencia
    ON eventos_proveedor (proveedor, id_externo_evento);
