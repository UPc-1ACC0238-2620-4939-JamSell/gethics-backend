-- Creación de la tabla de asignaciones veterinarias para Gethics
CREATE TABLE IF NOT EXISTS veterinary_assignments (
                                                      id BIGSERIAL PRIMARY KEY,
                                                      veterinarian_id BIGINT NOT NULL,
                                                      client_id BIGINT NOT NULL,
                                                      status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- Índice para optimizar las consultas de búsqueda por veterinario, cliente y estado
CREATE INDEX IF NOT EXISTS idx_vet_assignments_lookup
    ON veterinary_assignments(veterinarian_id, client_id, status);

