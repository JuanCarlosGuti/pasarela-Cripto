-- V1: esquema base de la pasarela.
-- Las tablas de cada contexto llegan con su historia de usuario
-- (V2: órdenes de pago, V3: comercios, ...), nunca por adelantado.

COMMENT ON SCHEMA public IS 'Pasarela de pagos cripto — esquema gestionado por Flyway';