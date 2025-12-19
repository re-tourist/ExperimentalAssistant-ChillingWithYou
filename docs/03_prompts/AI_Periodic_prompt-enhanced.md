好，我们把它当成“**最关键的系统工程**”来做：把所有 Prompt 统一强化成 **短、硬、可验收、不可跑偏** 的版本。你之后只需要固定喂：

> **AI_CONTEXT（不改） + 当前阶段 Prompt（下面之一）**

我会按 **6 个核心阶段**给你强化版，并额外给 2 个**加分阶段（Perf / Docs）**，用于穿插提交。

------

# 使用方式（固定）

每次发给 AI 的消息结构：

1. 粘贴 `AI_CONTEXT.md`（原样、不改）
2. 紧接着粘贴对应阶段 Prompt（下面复制即可）
3. 最后一行补一句：
   - “输出：文件清单 + 关键代码 + 启动命令 + 验收 curl/页面操作 + 建议 commit message”

------

# Backend-1 强化 Prompt（骨架 + 三表 CRUD）

```text
Stage: Backend-1 (Bootstrap + Base CRUD)

Goal
- Build a runnable Spring Boot backend skeleton.
- Implement base CRUD for ONLY: project, metric_def, tag.
- Do NOT implement run/run_metric/run_tag/dashboard in this stage.

Tech Constraints
- Spring Boot 3, Java 17, Maven
- MySQL + MyBatis-Plus
- Package structure MUST be: controller / service / mapper / entity / dto / config / common
- All REST endpoints MUST be under /api
- Unified response: { code:int, message:string, data:any }
- Pagination response: { records:[], total:number, page:number, size:number }

Deliverables (MUST)
1) backend/sql/schema.sql contains ONLY tables:
   - project, metric_def, tag
2) application.yml includes mysql config + server.port=8080
3) GET /health returns "ok"
4) Endpoints:
   - GET /api/projects?page&size&q
   - POST /api/projects
   - PUT /api/projects/{id}
   - DELETE /api/projects/{id}
   - GET /api/metrics/defs
   - POST /api/metrics/defs
   - GET /api/tags?q
   - POST /api/tags
5) Provide curl commands to verify each endpoint.

Rules
- No auth, no swagger, no extra frameworks.
- Provide full file paths and copy-paste-ready code.
- Keep naming consistent with schema.sql.
Suggested commit: "chore: bootstrap backend skeleton and base crud"
```

------

# Backend-2 强化 Prompt（Run 多表事务 + 校验 + 详情）

```text
Stage: Backend-2 (Run CRUD + Metrics/Tags Transactional)

Goal
- Add run, run_metric, run_tag tables and implement Run CRUD.
- Run create/update MUST be transactional: run + run_metric + run_tag.
- Return run detail including tags and metrics with metric_def info.

Tech Constraints
- Continue Backend-1 conventions unchanged.
- Use @Transactional on create/update methods.
- Enforce uniqueness: (run_id, metric_def_id) and (run_id, tag_id).
- Validate: metrics list MUST NOT contain duplicate metricDefId.
- Keep API response format unchanged.

Deliverables (MUST)
1) Update backend/sql/schema.sql to include ONLY these additional tables:
   - run, run_metric, run_tag
   (Keep project/metric_def/tag from previous stage)
2) DTOs:
   - RunCreateUpdateRequest:
     { projectId, runName, status, modelName, datasetName, optimizer, lr, batchSize, epochs, seed, note,
       tagIds:number[], metrics:[{metricDefId:number,value:number}] }
   - RunDetailResponse:
     run base fields + tags:[{id,name}] + metrics:[{metricDefId,name,displayName,direction,value}]
3) Endpoints:
   - GET /api/runs?page&size&projectId&status&q&dateFrom&dateTo&tagIds
   - GET /api/runs/{id}
   - POST /api/runs
   - PUT /api/runs/{id}
   - DELETE /api/runs/{id}
4) Behavior rules:
   - PUT is full replace update:
     update run base -> delete old run_metric/run_tag -> insert new batch
   - If duplicate metricDefId -> return code!=0 and message explains error
5) Provide curl smoke tests:
   - create run with 3 metrics (acc/loss/f1) + 2 tags
   - get detail
   - update run (change metric values/tags)
   - list with filters

Rules
- Do NOT implement dashboard in this stage.
- Prefer MyBatis-Plus for CRUD; join queries allowed for detail endpoint.
Suggested commit: "feat: run crud with metrics and tags transactional"
```

------

# Backend-3 强化 Prompt（Dashboard 聚合接口：Best-of-day / 分布 / TopN）

```text
Stage: Backend-3 (Dashboard Aggregation APIs)

Goal
- Implement dashboard aggregation endpoints:
  summary / trend / distribution / top
- Trend MUST be Best-of-day:
  direction=MAX -> daily MAX(value), direction=MIN -> daily MIN(value)

Tech Constraints
- Aggregation queries MUST be implemented via a dedicated DashboardMapper with custom SQL
  (XML or @Select). Do NOT force QueryWrapper for complex join/group.
- Support filters: projectId, dateFrom, dateTo, status, tagIds (tagIds uses EXISTS run_tag).
- Keep response format unchanged.

Deliverables (MUST)
1) Endpoints:
   - GET /api/dashboard/summary?projectId&dateFrom&dateTo&status&tagIds
   - GET /api/dashboard/trend?projectId&metricDefId&dateFrom&dateTo&status&tagIds&granularity=day
   - GET /api/dashboard/distribution?projectId&by=tag|model|dataset&dateFrom&dateTo&status
   - GET /api/dashboard/top?projectId&metricDefId&limit=10&dateFrom&dateTo&status
2) summary returns:
   { totalRuns, runsLast7Days, successRate, bestMetric:{metricDefId, metricName, value, runId} }
3) trend returns: [{ x:"YYYY-MM-DD", y:number }]
   - y is best-of-day using metric_def.direction
4) distribution returns: [{ name:string, value:number }]
5) top returns: [{ runId, runName, modelName, datasetName, value, createdAt, status }]

Rules
- Do NOT add auth/swagger.
- Provide curl examples for all endpoints.
Suggested commit: "feat: dashboard aggregation apis (summary trend distribution top)"
```

------

# Frontend-1 强化 Prompt（前端骨架 + Layout + API 层）

```text
Stage: Frontend-1 (Bootstrap + Layout + API Client)

Goal
- Build Vue3 frontend skeleton with admin layout and routing.
- Implement axios client and typed API modules matching backend spec.

Tech Constraints
- Vue 3 + Vite + Element Plus + Vue Router + Pinia + Axios
- ECharts will be used later, but in this stage only set up structure.
- Axios baseURL: http://localhost:8080
- Unified API handling:
  - if response.code != 0 -> ElMessage.error(message)
- Keep file structure clean:
  src/api, src/views, src/components, src/stores, src/utils

Deliverables (MUST)
1) Routes:
   - /dashboard, /runs, /projects, /about
2) Layout:
   - Sidebar menu + top bar + router-view
3) API layer:
   - src/api/types.ts defines ApiResponse, PageResult
   - src/api/projects.ts, runs.ts, dashboard.ts, tags.ts, metrics.ts
4) App runs with: npm install && npm run dev
5) All pages can open as placeholders without errors.

Rules
- No premature business logic.
Suggested commit: "chore: bootstrap frontend layout and api client"
```

------

# Frontend-2 强化 Prompt（Runs 页面 + 动态指标表单 + 详情抽屉）

```text
Stage: Frontend-2 (Runs Page + Dynamic Metrics Form)

Goal
- Implement Runs management page:
  list + filters + pagination + detail drawer + create/edit dialog
- Implement dynamic metrics form (core highlight).

Tech Constraints
- Use Element Plus components.
- Runs list MUST be paginated (no full loading).
- Metrics dynamic form MUST be table-based:
  - el-table over metrics array
  - each row: metricDefId (el-select) + value (el-input-number) + delete button
  - add row button
- Prevent duplicate metricDefId on frontend (validate before submit).
- Detail drawer MUST show tags and full metrics with direction/displayName.

Deliverables (MUST)
1) src/views/Runs.vue
   - filters: projectId, status, q, date range
   - table: runs list, loading, empty state
   - pagination controls
2) Create/Edit Dialog:
   fields: projectId, runName, status, modelName, datasetName, optimizer, lr, batchSize, epochs, seed, note, tagIds
   metrics dynamic table as specified
3) Detail Drawer:
   fetch GET /api/runs/{id}
   show base fields + tags + metrics table
4) Hook up APIs from src/api/runs.ts etc.

Rules
- Do NOT build dashboard here.
- Provide a short “manual test checklist” (create -> list -> detail -> edit -> delete).
Suggested commits:
- "feat: runs page list with filters and paging"
- "feat: run create/edit dialog with dynamic metrics form"
```

------

# Frontend-3 强化 Prompt（Dashboard 页面 + 图表 + 缓存 + Loading）

```text
Stage: Frontend-3 (Dashboard Page + Charts + Cache)

Goal
- Implement Dashboard page with filters + KPI + trend + distribution + TopN.
- Add loading/skeleton and a simple 30s TTL cache to improve UX.

Tech Constraints
- Use ECharts for charts.
- Trend chart MUST represent Best-of-day data from backend (/api/dashboard/trend).
- Add a small cache utility:
  - key = JSON.stringify(params)
  - ttl = 30 seconds
  - store in memory (Map)
- Handle states:
  - initial loading
  - empty data
  - request error -> ElMessage

Deliverables (MUST)
1) src/views/Dashboard.vue:
   - filters: projectId, date range, status, metricDefId, distributionBy(tag/model/dataset)
   - KPI cards: call /summary
   - trend line chart: call /trend
   - distribution chart: call /distribution
   - TopN table: call /top
2) src/utils/cache.ts implements TTL cache
3) Charts update when filters change (debounce optional)

Rules
- Don’t add fancy features beyond MVP.
Suggested commits:
- "feat: dashboard page (filters kpi charts topN)"
- "perf: add dashboard cache ttl and loading states"
```

------

# 加分阶段 Prompt（建议穿插）：Perf-UX 强化 Prompt

```text
Stage: Perf-UX (Loading / Empty / Error / Consistency)

Goal
- Polish UX and performance without changing business scope.

Deliverables (MUST)
- Add consistent loading indicators (skeleton or spinner) for Runs and Dashboard.
- Add Empty states for tables and charts.
- Standardize error handling (axios interceptors + ElMessage).
- Ensure pagination remains fast and smooth.

Rules
- No new features, only polishing.
Suggested commit: "perf: polish ux (loading empty error) and performance"
```

------

# 加分阶段 Prompt（建议最后）：Docs 强化 Prompt

```text
Stage: Docs (Deployment + Demo Script)

Goal
- Produce final course deliverables from existing implementation.

Deliverables (MUST)
- docs/04_delivery/部署说明.md:
  frontend/backed/db startup steps + common troubleshooting
- docs/04_delivery/验收演示脚本.md:
  5-10 min demo flow + what to say + which features/tech points to highlight
- docs/04_delivery/系统使用说明文档.md:
  basic user guide with screenshots placeholders
- docs/04_delivery/系统说明文档.md:
  architecture + database + api + key tech points (pagination, best-of-day trend, cache)

Rules
- Align wording with “ExperimentalAssistant” only.
Suggested commit: "docs: finalize deployment guide and demo script"
```

------

## 最关键的提醒（保证你不会翻车）

1. **阶段之间不要混写**：Backend-1 就别让 AI 写 run；Frontend-2 就别写 dashboard。
2. **每次让 AI 输出“文件清单 + 验收方式”**：你能立即判断对不对。
3. **发现偏航就立刻用一句话纠偏**：
   - “Stop. You added X which is forbidden by AI_CONTEXT. Remove it and regenerate only affected files.”

------

如果你现在就要开工，我建议你从 **Backend-1** 开始，并且在发给 AI 后把它生成的“文件清单 + schema.sql + controller 路径”贴回来（哪怕只贴目录树），我可以帮你做一次“快速审查”，避免你在第一个阶段就埋雷。