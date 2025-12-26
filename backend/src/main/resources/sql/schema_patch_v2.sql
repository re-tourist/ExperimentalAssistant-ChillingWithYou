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
