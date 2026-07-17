-- V12: banco/billetera de la cuenta de liquidación (HU-027).
-- Antes el "tipo" mezclaba dos conceptos: DÓNDE está la cuenta (Nequi,
-- Bancolombia...) y QUÉ tipo es (ahorros/corriente). El proveedor de payout
-- va a exigir el banco, así que se separan desde ya.
-- Migración de datos: los tipo=NEQUI existentes pasan a banco='Nequi' con
-- tipo=AHORROS (las billeteras se manejan como ahorros por convención);
-- el resto queda con banco='No registrado' hasta que el comercio lo declare.

ALTER TABLE comercios ADD COLUMN cuenta_banco VARCHAR(50) NOT NULL DEFAULT 'No registrado';

UPDATE comercios SET cuenta_banco = 'Nequi', cuenta_tipo = 'AHORROS' WHERE cuenta_tipo = 'NEQUI';

ALTER TABLE comercios ALTER COLUMN cuenta_banco DROP DEFAULT;
