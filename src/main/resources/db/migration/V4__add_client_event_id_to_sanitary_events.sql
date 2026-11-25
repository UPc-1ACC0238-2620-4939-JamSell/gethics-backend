ALTER TABLE sanitary_events
    ADD COLUMN IF NOT EXISTS client_event_id VARCHAR(64);

ALTER TABLE sanitary_events
    ADD CONSTRAINT uq_sanitary_events_client_event_id UNIQUE (client_event_id);