CREATE TABLE employees (
    id          CHAR(36)     NOT NULL,
    full_name   VARCHAR(200) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email)
);

CREATE INDEX idx_employees_email ON employees (email);
CREATE INDEX idx_employees_department ON employees (department);
