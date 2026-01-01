SET @sql_template_domain :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
            AND table_name = 'template'
            AND column_name = 'domain'
        ),
        'SELECT 1',
        'ALTER TABLE template ADD COLUMN domain VARCHAR(32) DEFAULT \'general\' COMMENT ''Domain'''
    ));
PREPARE stmt_template_domain FROM @sql_template_domain;
EXECUTE stmt_template_domain;
DEALLOCATE PREPARE stmt_template_domain;

SET @sql_template_is_default :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
            AND table_name = 'template'
            AND column_name = 'is_default'
        ),
        'SELECT 1',
        'ALTER TABLE template ADD COLUMN is_default TINYINT(1) DEFAULT 0 COMMENT ''Is Default Template'''
    ));
PREPARE stmt_template_is_default FROM @sql_template_is_default;
EXECUTE stmt_template_is_default;
DEALLOCATE PREPARE stmt_template_is_default;

-- Create template_field table
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

SET @sql_template_field_is_group_by :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'template_field'
              AND column_name = 'is_group_by'
        ),
        'SELECT 1',
        'ALTER TABLE template_field ADD COLUMN is_group_by TINYINT(1) DEFAULT 0 COMMENT ''Is GroupBy field'''
    ));
PREPARE stmt_template_field_is_group_by FROM @sql_template_field_is_group_by;
EXECUTE stmt_template_field_is_group_by;
DEALLOCATE PREPARE stmt_template_field_is_group_by;

-- Create run_field_value table
CREATE TABLE IF NOT EXISTS run_field_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    value_text TEXT,
    INDEX idx_run_id (run_id),
    UNIQUE KEY uk_run_field (run_id, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql_run_start_time :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'run'
              AND column_name = 'start_time'
        ),
        'SELECT 1',
        'ALTER TABLE run ADD COLUMN start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''Run Start Time'''
    ));
PREPARE stmt_run_start_time FROM @sql_run_start_time;
EXECUTE stmt_run_start_time;
DEALLOCATE PREPARE stmt_run_start_time;

SET @sql_run_end_time :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'run'
              AND column_name = 'end_time'
        ),
        'SELECT 1',
        'ALTER TABLE run ADD COLUMN end_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''Run End Time'''
    ));
PREPARE stmt_run_end_time FROM @sql_run_end_time;
EXECUTE stmt_run_end_time;
DEALLOCATE PREPARE stmt_run_end_time;

SET @sql_run_drop_created_at :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'run'
              AND column_name = 'created_at'
        ),
        'ALTER TABLE run DROP COLUMN created_at',
        'SELECT 1'
    ));
PREPARE stmt_run_drop_created_at FROM @sql_run_drop_created_at;
EXECUTE stmt_run_drop_created_at;
DEALLOCATE PREPARE stmt_run_drop_created_at;

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL COMMENT 'user, ai, system',
    content MEDIUMTEXT NOT NULL,
    meta_json TEXT COMMENT 'References',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql_project_template_id :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'project'
              AND column_name = 'template_id'
        ),
        'SELECT 1',
        'ALTER TABLE project ADD COLUMN template_id BIGINT NULL COMMENT ''Template ID'''
    ));
PREPARE stmt_project_template_id FROM @sql_project_template_id;
EXECUTE stmt_project_template_id;
DEALLOCATE PREPARE stmt_project_template_id;

SET @sql_project_config_snapshot :=
    (SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'project'
              AND column_name = 'project_config_snapshot'
        ),
        'SELECT 1',
        'ALTER TABLE project ADD COLUMN project_config_snapshot TEXT NULL COMMENT ''Project Config Snapshot'''
    ));
PREPARE stmt_project_config_snapshot FROM @sql_project_config_snapshot;
EXECUTE stmt_project_config_snapshot;
DEALLOCATE PREPARE stmt_project_config_snapshot;
