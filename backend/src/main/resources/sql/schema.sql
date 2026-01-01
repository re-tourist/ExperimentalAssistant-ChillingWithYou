CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT 'Project Name',
    description TEXT COMMENT 'Project Description',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS metric_def (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Metric Name',
    direction VARCHAR(10) NOT NULL COMMENT 'Optimization Direction: MAX or MIN',
    description VARCHAR(255) COMMENT 'Metric Description',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Tag Name',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT 'Project ID',
    name VARCHAR(255) NOT NULL COMMENT 'Run Name',
    status VARCHAR(50) DEFAULT 'RUNNING' COMMENT 'Status: RUNNING, FINISHED, FAILED',
    model_name VARCHAR(255) COMMENT 'Model Architecture Name',
    dataset_name VARCHAR(255) COMMENT 'Dataset Name',
    optimizer VARCHAR(50) COMMENT 'Optimizer Name',
    lr DOUBLE COMMENT 'Learning Rate',
    batch_size INT COMMENT 'Batch Size',
    epochs INT COMMENT 'Total Epochs',
    seed INT COMMENT 'Random Seed',
    note TEXT COMMENT 'Run Note',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Run Start Time',
    end_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Run End Time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    metric_def_id BIGINT NOT NULL,
    value DOUBLE NOT NULL,
    UNIQUE KEY uk_run_metric (run_id, metric_def_id),
    INDEX idx_run_id (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_run_tag (run_id, tag_id),
    INDEX idx_run_id (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE COMMENT 'Template Name',
    domain VARCHAR(32) DEFAULT 'general' COMMENT 'Domain',
    description VARCHAR(255) COMMENT 'Description',
    config_json TEXT COMMENT 'Configuration JSON',
    is_default TINYINT(1) DEFAULT 0 COMMENT 'Is Default Template',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    field_key VARCHAR(64) NOT NULL COMMENT 'Field Key (e.g. learning_rate)',
    label VARCHAR(64) NOT NULL COMMENT 'Display Label',
    field_type VARCHAR(32) NOT NULL COMMENT 'TEXT, NUMBER, SELECT, BOOLEAN, TEXTAREA',
    is_required TINYINT(1) DEFAULT 0,
    is_group_by TINYINT(1) DEFAULT 0 COMMENT 'Is GroupBy field',
    default_value TEXT COMMENT 'Default Value',
    sort_order INT DEFAULT 0,
    options_json TEXT COMMENT 'JSON array for SELECT options',
    unit VARCHAR(32) COMMENT 'Unit string',
    placeholder VARCHAR(128) COMMENT 'Input placeholder',
    UNIQUE KEY uk_template_field (template_id, field_key),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run_field_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    value_text TEXT,
    INDEX idx_run_id (run_id),
    UNIQUE KEY uk_run_field (run_id, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_metric_def (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    metric_def_id BIGINT NOT NULL,
    is_default TINYINT(1) DEFAULT 0 COMMENT 'Is Default',
    sort_order INT DEFAULT 0 COMMENT 'Sort Order',
    UNIQUE KEY uk_template_metric (template_id, metric_def_id),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    is_default TINYINT(1) DEFAULT 0 COMMENT 'Is Default',
    sort_order INT DEFAULT 0 COMMENT 'Sort Order',
    UNIQUE KEY uk_template_tag (template_id, tag_id),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS run_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL COMMENT 'Run ID',
    type VARCHAR(16) NOT NULL COMMENT 'Type: NOTE, CONCLUSION, AI_DRAFT',
    title VARCHAR(128) NULL COMMENT 'Optional Title',
    content_md MEDIUMTEXT NOT NULL COMMENT 'Markdown Content',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_run_id (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS domain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE COMMENT 'Domain Name',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
