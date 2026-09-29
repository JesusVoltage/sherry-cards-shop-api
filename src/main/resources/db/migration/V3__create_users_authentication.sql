CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE estados_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_estados_usuario PRIMARY KEY (id),
    CONSTRAINT uk_estados_usuario_code UNIQUE (code)
);

INSERT INTO roles (code, name)
VALUES ('CLIENTE', 'Cliente'), ('ADMIN', 'Administrador');

INSERT INTO estados_usuario (code, name)
VALUES ('PENDIENTE', 'Pendiente'), ('ACTIVO', 'Activo'), ('BLOQUEADO', 'Bloqueado');

CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    username VARCHAR(30) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150),
    password_hash VARCHAR(255),
    google_sub VARCHAR(255),
    rol_id BIGINT NOT NULL,
    estado_id BIGINT NOT NULL,
    email_verificado_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ultimo_acceso_at DATETIME(6),
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT uk_usuarios_google_sub UNIQUE (google_sub),
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_usuarios_estados FOREIGN KEY (estado_id) REFERENCES estados_usuario (id),
    CONSTRAINT ck_usuarios_credencial CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL)
);

CREATE INDEX idx_usuarios_estado ON usuarios (estado_id);

CREATE TABLE direcciones_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    alias VARCHAR(100),
    nombre_destinatario VARCHAR(100) NOT NULL,
    apellidos_destinatario VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    calle VARCHAR(200) NOT NULL,
    numero VARCHAR(30) NOT NULL,
    complemento VARCHAR(150),
    codigo_postal VARCHAR(20) NOT NULL,
    localidad VARCHAR(100) NOT NULL,
    provincia VARCHAR(100) NOT NULL,
    pais VARCHAR(100) NOT NULL,
    uso_envio BOOLEAN NOT NULL DEFAULT TRUE,
    uso_facturacion BOOLEAN NOT NULL DEFAULT FALSE,
    predeterminada_envio BOOLEAN NOT NULL DEFAULT FALSE,
    predeterminada_facturacion BOOLEAN NOT NULL DEFAULT FALSE,
    predeterminada_envio_usuario_id BIGINT AS (CASE WHEN predeterminada_envio THEN usuario_id ELSE NULL END),
    predeterminada_facturacion_usuario_id BIGINT AS (CASE WHEN predeterminada_facturacion THEN usuario_id ELSE NULL END),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_direcciones_usuario PRIMARY KEY (id),
    CONSTRAINT fk_direcciones_usuario_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_direcciones_uso CHECK (uso_envio OR uso_facturacion),
    CONSTRAINT ck_direcciones_predeterminada_envio CHECK (NOT predeterminada_envio OR uso_envio),
    CONSTRAINT ck_direcciones_predeterminada_facturacion CHECK (NOT predeterminada_facturacion OR uso_facturacion),
    CONSTRAINT uk_direcciones_usuario_predeterminada_envio UNIQUE (predeterminada_envio_usuario_id),
    CONSTRAINT uk_direcciones_usuario_predeterminada_facturacion UNIQUE (predeterminada_facturacion_usuario_id)
);

CREATE INDEX idx_direcciones_usuario_usuario ON direcciones_usuario (usuario_id);

CREATE TABLE tokens_autenticacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    CONSTRAINT pk_tokens_autenticacion PRIMARY KEY (id),
    CONSTRAINT uk_tokens_autenticacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_autenticacion_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_tokens_autenticacion_usuario ON tokens_autenticacion (usuario_id);
CREATE INDEX idx_tokens_autenticacion_expiry ON tokens_autenticacion (expires_at, revoked_at);
