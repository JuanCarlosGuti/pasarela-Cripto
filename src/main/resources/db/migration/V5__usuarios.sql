-- V5: cuentas de acceso a la plataforma (HU-006).
-- Solo se guarda el HASH BCrypt de la contraseña, jamás la contraseña.

CREATE TABLE usuarios (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(150) NOT NULL,
    hash_contrasena VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    comercio_id     UUID,
    creado_en       TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (email);
