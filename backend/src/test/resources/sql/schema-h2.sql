CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description CLOB,
    template_id BIGINT,
    project_config_snapshot CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS metric_def (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    direction VARCHAR(10) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'RUNNING',
    model_name VARCHAR(255),
    dataset_name VARCHAR(255),
    optimizer VARCHAR(50),
    lr DOUBLE,
    batch_size INT,
    epochs INT,
    seed INT,
    note CLOB,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_run_project_id ON run(project_id);

CREATE TABLE IF NOT EXISTS run_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    metric_def_id BIGINT NOT NULL,
    value DOUBLE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_run_metric ON run_metric(run_id, metric_def_id);
CREATE INDEX IF NOT EXISTS idx_run_metric_run_id ON run_metric(run_id);

CREATE TABLE IF NOT EXISTS run_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_run_tag ON run_tag(run_id, tag_id);
CREATE INDEX IF NOT EXISTS idx_run_tag_run_id ON run_tag(run_id);

CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    domain VARCHAR(32) DEFAULT 'general',
    description VARCHAR(255),
    config_json CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS template_metric_def (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    metric_def_id BIGINT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_template_metric ON template_metric_def(template_id, metric_def_id);
CREATE INDEX IF NOT EXISTS idx_template_metric_template_id ON template_metric_def(template_id);

CREATE TABLE IF NOT EXISTS template_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_template_tag ON template_tag(template_id, tag_id);
CREATE INDEX IF NOT EXISTS idx_template_tag_template_id ON template_tag(template_id);

CREATE TABLE IF NOT EXISTS run_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    title VARCHAR(128),
    content_md CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_run_note_run_id ON run_note(run_id);
