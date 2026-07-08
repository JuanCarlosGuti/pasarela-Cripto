-- V2: órdenes de pago y su historial de transiciones (T-005).

CREATE TABLE ordenes (
    id          UUID          PRIMARY KEY,
    comercio_id UUID          NOT NULL,
    monto       NUMERIC(19,4) NOT NULL,
    moneda      VARCHAR(10)   NOT NULL,
    referencia  VARCHAR(64)   NOT NULL,
    estado      VARCHAR(20)   NOT NULL,
    creada_en   TIMESTAMPTZ   NOT NULL,
    expira_en   TIMESTAMPTZ   NOT NULL
);

-- La referencia casa el webhook con la orden: única por diseño (T-005)
CREATE UNIQUE INDEX ux_ordenes_referencia ON ordenes (referencia);

-- Insumo del job de expiración (HU-014): pendientes vencidas
CREATE INDEX ix_ordenes_estado_expira_en ON ordenes (estado, expira_en);

-- Historial de transiciones: auditoría de cada cambio de estado
CREATE TABLE orden_transiciones (
    orden_id UUID         NOT NULL REFERENCES ordenes (id),
    indice   INTEGER      NOT NULL,
    desde    VARCHAR(20)  NOT NULL,
    hacia    VARCHAR(20)  NOT NULL,
    momento  TIMESTAMPTZ  NOT NULL,
    motivo   VARCHAR(500),
    PRIMARY KEY (orden_id, indice)
);
