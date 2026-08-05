CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    model_used VARCHAR(100),
    decision_mode VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    consensus_result VARCHAR(20),
    winning_unit VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
