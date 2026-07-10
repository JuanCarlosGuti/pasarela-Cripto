-- V8: bloqueo optimista en la orden (HU-014).
-- La carrera expiración-vs-pago se resuelve con control de versión: si dos
-- transacciones cargan la misma orden y ambas intentan transicionarla, solo
-- la primera en confirmar gana; la otra recibe un fallo optimista y se
-- rechaza limpiamente (el job la salta; el webhook recarga y reintenta).

ALTER TABLE ordenes ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
