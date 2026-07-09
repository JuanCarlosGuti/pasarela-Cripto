-- V3: comercios y su cuenta de liquidación (HU-004).
-- La cuenta es SIEMPRE del comercio (regla de oro: la plataforma nunca
-- custodia fondos; el proveedor liquida directo a esta cuenta).

CREATE TABLE comercios (
    id                  UUID         PRIMARY KEY,
    razon_social        VARCHAR(200) NOT NULL,
    nit                 VARCHAR(20)  NOT NULL,
    estado_verificacion VARCHAR(20)  NOT NULL,
    cuenta_tipo         VARCHAR(20)  NOT NULL,
    cuenta_numero       VARCHAR(30)  NOT NULL,
    cuenta_titular      VARCHAR(200) NOT NULL,
    registrado_en       TIMESTAMPTZ  NOT NULL
);

-- Un NIT solo puede registrarse una vez (HU-004: duplicado → 409)
CREATE UNIQUE INDEX ux_comercios_nit ON comercios (nit);