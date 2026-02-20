-- 1. Таблица документов
CREATE TABLE documents
(
    id            UUID PRIMARY KEY,
    unique_number VARCHAR(255) NOT NULL UNIQUE,
    author        VARCHAR(255) NOT NULL,
    title         VARCHAR(255),
    status        VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP,

-- Поле для Optimistic Locking
    version       BIGINT DEFAULT 0
);

-- Индексы для быстрого поиска
CREATE INDEX idx_docs_status ON documents (status);
CREATE INDEX idx_docs_author ON documents (author);
CREATE INDEX idx_docs_created_at ON documents (created_at);

-- 2. Таблица истории изменений
CREATE TABLE document_history
(
    id          UUID PRIMARY KEY,
    document_id UUID         NOT NULL,
    initiator   VARCHAR(255) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    comment     TEXT,

    CONSTRAINT fk_history_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

-- Индекс для получения истории конкретного документа
CREATE INDEX idx_history_doc_id ON document_history (document_id);

-- 3. Реестр утвержденных документов
CREATE TABLE registry_entries
(
    id            UUID PRIMARY KEY,
    document_id   UUID      NOT NULL,
    registered_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_registry_doc UNIQUE (document_id),
    CONSTRAINT fk_registry_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

