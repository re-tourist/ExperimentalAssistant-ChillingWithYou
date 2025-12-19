# AI_CONTEXT

【Project Context】
This is a course project focused on a data-driven web system.
The core is a Data Visualization Dashboard, supported by resource/content management.

Theme: Project / Experiment Metrics (deep learning style).
The system manages experiment runs and visualizes metrics such as acc, loss, f1.

【Architecture Decisions (Frozen)】
- Backend: Spring Boot 3 + MyBatis-Plus + MySQL
- Frontend: Vue 3 + Element Plus
- CRUD and aggregation queries are separated:
  - CRUD uses MyBatis-Plus
  - Dashboard aggregation uses custom Mapper SQL
- Core entities:
  - project
  - run
  - metric_def (defines metric name and direction MAX/MIN)
  - run_metric (multiple metrics per run)
  - tag / run_tag
- Run create/update must be transactional (run + run_metric + run_tag).

【Dashboard Rules】
- Metrics: acc (MAX), loss (MIN), f1 (MAX)
- Trend chart uses Best-of-day:
  - MAX(value) if direction=MAX
  - MIN(value) if direction=MIN
- Distribution by: model / dataset / tag
- TopN sorted by metric direction

【API Conventions】
- Unified response: { code, message, data }
- Pagination response: { records, total, page, size }
- No authentication/permission system in MVP.
- No CSV import, no real-time, no workflow.

【Scope Control】
This is MVP only. Do NOT add:
- RBAC or permission frameworks
- message queues
- WebSocket
- training or model logic