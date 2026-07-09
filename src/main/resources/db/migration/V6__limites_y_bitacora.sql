-- V6: límites de operación del comercio y bitácora de operaciones
-- inusuales (HU-007, controles de cumplimiento del MVP).
-- Montos en COP (el MVP opera solo en pesos; la moneda del cobro vive en
-- la orden). Las filas existentes reciben los topes por defecto del MVP.

ALTER TABLE comercios ADD COLUMN limite_por_transaccion NUMERIC(19,4) NOT NULL DEFAULT 2000000;
ALTER TABLE comercios ADD COLUMN limite_mensual NUMERIC(19,4) NOT NULL DEFAULT 20000000;

CREATE TABLE bitacora_operaciones (
    id      UUID          PRIMARY KEY,
    momento TIMESTAMPTZ   NOT NULL,
    tipo    VARCHAR(50)   NOT NULL,
    actor   VARCHAR(150)  NOT NULL,
    detalle VARCHAR(1000) NOT NULL
);

CREATE INDEX ix_bitacora_momento ON bitacora_operaciones (momento);
