-- V10: detalle de la discrepancia de conciliación (HU-017).
-- Cuando lo registrado no coincide con lo reportado por el proveedor, la
-- diferencia queda escrita — jamás se cuadra en silencio.

ALTER TABLE liquidaciones ADD COLUMN detalle_discrepancia VARCHAR(1000);
