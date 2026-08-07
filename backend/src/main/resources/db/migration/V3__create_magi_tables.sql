CREATE TABLE magi_verdicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id),
    unit VARCHAR(20) NOT NULL,
    round INT NOT NULL,
    answer_text TEXT,
    voted_for_unit VARCHAR(20),
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE magi_syntheses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id),
    common_ground TEXT,
    differences TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_magi_verdicts_message_id ON magi_verdicts(message_id);
