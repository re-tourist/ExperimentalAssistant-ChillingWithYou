# DB_SCHEMA — 数据库结构设计

项目名称：ExperimentalAssistant  
数据库类型：MySQL

---

## 一、设计原则

- 一次实验（Run）支持多个指标（Metric）
- 指标定义与指标值分离，支持 MAX / MIN 方向
- 支持标签多对多关联
- 结构贴近真实深度学习实验管理系统

---

## 二、数据表设计

### 1. project（项目）

- id BIGINT PK
- name VARCHAR(64)
- description VARCHAR(255)
- created_at DATETIME
- updated_at DATETIME

---

### 2. run（实验记录）

- id BIGINT PK
- project_id BIGINT FK
- run_name VARCHAR(128)
- status VARCHAR(16)
- model_name VARCHAR(64)
- dataset_name VARCHAR(64)
- optimizer VARCHAR(32)
- lr DOUBLE
- batch_size INT
- epochs INT
- seed INT
- note VARCHAR(255)
- created_at DATETIME
- updated_at DATETIME

---

### 3. metric_def（指标定义）

- id BIGINT PK
- name VARCHAR(32) UNIQUE
- display_name VARCHAR(64)
- direction ENUM('MAX','MIN')
- created_at DATETIME

示例：
- acc（MAX）
- loss（MIN）
- f1（MAX）

---

### 4. run_metric（实验指标值）

- id BIGINT PK
- run_id BIGINT FK
- metric_def_id BIGINT FK
- value DOUBLE
- created_at DATETIME

约束：
- UNIQUE (run_id, metric_def_id)

---

### 5. tag（标签）

- id BIGINT PK
- name VARCHAR(32) UNIQUE
- created_at DATETIME

---

### 6. run_tag（实验-标签关联）

- id BIGINT PK
- run_id BIGINT FK
- tag_id BIGINT FK

约束：
- UNIQUE (run_id, tag_id)

---

## 三、说明

- run_metric 表用于支持一个实验对应多个指标
- metric_def.direction 用于 Dashboard 聚合逻辑判断（MAX / MIN）
- 本版本不包含导入任务表，批量导入属于未来扩展方向