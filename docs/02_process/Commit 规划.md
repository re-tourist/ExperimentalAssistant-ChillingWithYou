# Commit 规划

## 1）把 commit 阶段细化：基于 6 段提示词 + 穿插优化/文档提示词

原则：

- **每段 AI Prompt 至少 1 个主 commit**（能跑、能验收）
- 中间插入 **Perf/Docs Prompt** 做“加分点”，不影响主线推进
- 最终形成一条老师看起来很舒服的提交链

### 建议的里程碑提交规划（推荐直接写进《整体开发流程.md》）

#### M0（可选但强烈建议）：仓库与文档骨架先落地

- `docs: add project docs skeleton (IA/DB/API/Process/AI_CONTEXT)`
- `chore: init repo structure (frontend/backend/docs)`

> 这一步能证明你们“先设计再开发”。

------

#### Backend-1（对应第 1 段 Prompt）

- `chore: bootstrap backend (spring boot, mysql, mybatis-plus, health)`
- `feat: add base response wrapper and paging models`

> 拆成 2 个 commit 的好处：出了问题更好回滚。

------

#### Backend-1.5（穿插：数据表与字典种子数据）

- `feat: add schema.sql and seed metric_def (acc/loss/f1) + tags`

> 这一步让后面前端/看板演示不空。

------

#### Backend-2（对应第 2 段 Prompt）

- `feat: run CRUD with metrics and tags (transactional)`
- `fix: validate metrics uniqueness and error handling`

> 事务 + 校验属于“技术含量”，建议至少一个独立 commit。

------

#### Backend-2.5（穿插：接口联调稳定性）

- `perf: optimize run list query and add metricPreview`
- `docs: add curl collection for backend api smoke test`

> 这一步非常“老师友好”：你不仅写了接口，还能证明你测过。

------

#### Backend-3（对应第 3 段 Prompt）

- `feat: dashboard aggregation apis (summary/trend/distribution/top)`
- `perf: dashboard aggregation uses custom mapper sql`

> 统计聚合是核心亮点，建议把“自定义 SQL/Mapper”作为 perf 或 feat 的独立描述。

------

#### Frontend-1（对应第 4 段 Prompt）

- `chore: bootstrap frontend (vue3/vite/element-plus/router/pinia)`
- `feat: add axios client and api modules`

------

#### Frontend-2（对应第 5 段 Prompt）

- `feat: runs page list with filters and paging`
- `feat: run create/edit dialog with dynamic metrics form`
- `fix: prevent duplicated metric selection on frontend`

> 这里建议拆 2–3 个 commit，因为“列表”和“动态表单”是两个大块。

------

#### Frontend-3（对应第 6 段 Prompt）

- `feat: dashboard page (filters + KPI + charts + topN)`
- `perf: add dashboard cache ttl and loading/skeleton states`

------

#### 收尾（穿插的“高分必备”）

- `perf: polish ux (empty states, error prompts, consistent loading)`
- `docs: add deployment guide and demo script`
- `docs: finalize report screenshots and feature checklist`

> 你原来的 6 段 Prompt 很好，但**最后一定要有 perf + docs 的 commit**，老师看过程分会很舒服。AI 开发流程

------

### “一段提示词交一次吗？”——最稳答案

- **至少一次**（每段结束必须有一个“可运行主 commit”）
- 但像 Frontend-2/3 这种大段，**允许拆成 2–3 个 commit**，反而更专业、更安全。