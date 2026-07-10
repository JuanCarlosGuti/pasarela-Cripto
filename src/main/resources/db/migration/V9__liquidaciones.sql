-- V9: liquidaciones (HU-016). Registro del dinero que el proveedor liquidó
-- DIRECTAMENTE al comercio: la plataforma jamás lo toca (REGLA DE ORO).
-- Una orden pertenece a UNA sola liquidación: orden_id es único global.

CREATE TABLE liquidaciones (
    id                   UUID          PRIMARY KEY,
    comercio_id          UUID          NOT NULL,
    monto_bruto          NUMERIC(19,4) NOT NULL,
    comision_plataforma  NUMERIC(19,4) NOT NULL,
    monto_neto_comercio  NUMERIC(19,4) NOT NULL,
    referencia_proveedor VARCHAR(100)  NOT NULL,
    estado               VARCHAR(20)   NOT NULL,
    liquidada_en         TIMESTAMPTZ   NOT NULL
);

CREATE TABLE liquidacion_ordenes (
    liquidacion_id UUID NOT NULL REFERENCES liquidaciones (id),
    orden_id       UUID NOT NULL,
    PRIMARY KEY (liquidacion_id, orden_id)
);

-- HU-016: una orden no puede pertenecer a dos liquidaciones
CREATE UNIQUE INDEX ux_liquidacion_ordenes_orden ON liquidacion_ordenes (orden_id);
