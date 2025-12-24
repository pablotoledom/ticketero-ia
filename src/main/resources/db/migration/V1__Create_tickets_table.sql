-- Migración inicial: Crear tabla tickets
-- Versión: V1__Create_tickets_table.sql

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia VARCHAR(20) NOT NULL UNIQUE,
    numero VARCHAR(10) NOT NULL,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    branch_office VARCHAR(50) NOT NULL,
    queue_type VARCHAR(20) NOT NULL CHECK (queue_type IN ('PREFERENCIAL', 'GENERAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    position_in_queue INTEGER NOT NULL DEFAULT 0,
    estimated_wait_minutes INTEGER NOT NULL DEFAULT 0,
    assigned_advisor VARCHAR(100),
    assigned_module_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar consultas
CREATE INDEX idx_tickets_codigo_referencia ON tickets(codigo_referencia);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_queue_type ON tickets(queue_type);
CREATE INDEX idx_tickets_branch_office ON tickets(branch_office);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para actualizar updated_at
CREATE TRIGGER update_tickets_updated_at 
    BEFORE UPDATE ON tickets 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();