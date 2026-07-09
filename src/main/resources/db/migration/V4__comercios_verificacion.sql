-- V4: última decisión de verificación del comercio (HU-005).
-- Motivo y momento de la decisión del Admin (rechazo/suspensión son auditables).

ALTER TABLE comercios ADD COLUMN motivo_decision VARCHAR(500);
ALTER TABLE comercios ADD COLUMN decision_en TIMESTAMPTZ;