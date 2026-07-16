-- V11: detalle de la conversión de la rampa (HU-025, ADR-006).
-- SOLO SIMULACIÓN: hasta que T-007 cierre con un proveedor real, este
-- desglose lo arma ProveedorDeRampaSimulado, no un proveedor de verdad.
-- Default 0/1/'' para que las liquidaciones ya registradas sigan cuadrando
-- (comision_rampa=0 no cambia el invariante bruto = comision + neto).

ALTER TABLE liquidaciones ADD COLUMN comision_rampa NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE liquidaciones ADD COLUMN tasa_cambio_simulada NUMERIC(19,4) NOT NULL DEFAULT 1;
ALTER TABLE liquidaciones ADD COLUMN cuenta_destino_descripcion VARCHAR(200) NOT NULL DEFAULT '';
