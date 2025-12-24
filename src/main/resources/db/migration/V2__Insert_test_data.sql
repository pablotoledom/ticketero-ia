-- Migración: Insertar datos de prueba
-- Versión: V2__Insert_test_data.sql

-- Datos de prueba para diferentes escenarios
INSERT INTO tickets (codigo_referencia, numero, national_id, telefono, branch_office, queue_type, status, position_in_queue, estimated_wait_minutes) VALUES
-- Tickets en espera - Cola General
('REF001', 'G001', '12345678', '555-0001', 'Sucursal Centro', 'GENERAL', 'WAITING', 1, 15),
('REF002', 'G002', '87654321', '555-0002', 'Sucursal Centro', 'GENERAL', 'WAITING', 2, 30),
('REF003', 'G003', '11223344', '555-0003', 'Sucursal Norte', 'GENERAL', 'WAITING', 1, 20),

-- Tickets en espera - Cola Preferencial
('REF004', 'P001', '44332211', '555-0004', 'Sucursal Centro', 'PREFERENCIAL', 'WAITING', 1, 10),
('REF005', 'P002', '55667788', '555-0005', 'Sucursal Sur', 'PREFERENCIAL', 'WAITING', 2, 15),

-- Tickets en progreso
('REF006', 'G004', '99887766', '555-0006', 'Sucursal Centro', 'GENERAL', 'IN_PROGRESS', 0, 0, 'Ana García', 1),
('REF007', 'P003', '66778899', '555-0007', 'Sucursal Norte', 'PREFERENCIAL', 'IN_PROGRESS', 0, 0, 'Carlos López', 2),

-- Tickets completados
('REF008', 'G005', '33445566', '555-0008', 'Sucursal Sur', 'GENERAL', 'COMPLETED', 0, 0, 'María Rodríguez', 3),
('REF009', 'P004', '77889900', '555-0009', 'Sucursal Centro', 'PREFERENCIAL', 'COMPLETED', 0, 0, 'Juan Pérez', 1);