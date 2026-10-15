CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

-- The states (UF) an analyst covers
CREATE TABLE user_covered_states (
    user_id BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    state   VARCHAR(2) NOT NULL,
    PRIMARY KEY (user_id, state)
);

CREATE TABLE solicitations (
    id               BIGSERIAL PRIMARY KEY,
    owner_id         BIGINT        NOT NULL REFERENCES users (id),
    status           VARCHAR(20)   NOT NULL,
    current_step     INTEGER       NOT NULL,
    -- Step 1: basic data
    service_type     VARCHAR(20),
    title            VARCHAR(80),
    description      VARCHAR(1000),
    -- Step 2: address
    cep              VARCHAR(8),
    cep_validated    BOOLEAN       NOT NULL DEFAULT FALSE,
    street           VARCHAR(200),
    neighborhood     VARCHAR(120),
    city             VARCHAR(120),
    state            VARCHAR(2),
    number           VARCHAR(20),
    complement       VARCHAR(120),
    -- Step 3: confirmation
    priority         VARCHAR(10),
    preferred_date   DATE,
    estimated_value  NUMERIC(12, 2),
    terms_accepted   BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Review
    analyst_id       BIGINT REFERENCES users (id),
    decision_comment VARCHAR(1000),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    submitted_at     TIMESTAMPTZ,
    decided_at       TIMESTAMPTZ,
    version          BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_solicitations_owner ON solicitations (owner_id);

CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    action        VARCHAR(50) NOT NULL,
    actor_id      BIGINT,
    resource_id   BIGINT,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_id);
