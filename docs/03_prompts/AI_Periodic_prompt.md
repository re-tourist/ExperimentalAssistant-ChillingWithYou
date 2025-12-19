# AI 开发流程

下面进入 **Step 12：AI 开发分步 Prompt 规范**。目标是：**让你“主要靠 AI 写代码”也能不散架**——目录统一、命名统一、接口一致、每一步可运行可提交。

我会给你两份 Prompt：

1. **后端 Spring Boot（MyBatis-Plus）Prompt**
2. **前端 Vue3（Element Plus）Prompt**

并给出 **6 个里程碑步骤**，你可以按步喂给 AI（每步结束都能运行 + commit）。

------

# Step 12.0 总规则（你必须复制给 AI 的“总约束”）

把下面这段放在每次给 AI 的消息开头（非常重要）：

> **全局约束**
>
> - 技术栈固定：Spring Boot 3 + MyBatis-Plus + MySQL；Vue3 + Vite + Element Plus + Vue Router + Pinia + Axios；图表用 ECharts。
> - API 契约固定：所有返回 `{code, message, data}`；分页返回 `{records,total,page,size}`。
> - 数据模型固定：project / run / metric_def / run_metric / tag / run_tag。
> - Run 更新必须事务：run + run_metric + run_tag 一起更新。
> - Dashboard 的 trend 使用 **Best-of-day**：direction=MAX 用 MAX(value)，direction=MIN 用 MIN(value)。
> - 只实现 MVP：Projects、Runs（含 metrics 动态）、Dashboard（summary/trend/distribution/top）。不要扩展权限系统/导入/复杂工作流。
> - 给出完整文件路径、文件名、类名；所有代码可直接复制粘贴运行。
> - 每一步都要给出：新增/修改文件清单 + 关键代码 + 启动方式 + 简单测试方式（curl 或前端页面）。

------

# Step 12.1 里程碑拆分（6步，步步可跑）

你按这个顺序喂给 AI，绝大概率不会翻车：

1. **Backend-1：骨架 + DB + 实体 + CRUD（不含 dashboard）**
2. **Backend-2：Run 创建/更新事务（含 metrics/tags 维护）**
3. **Backend-3：Dashboard 聚合接口（summary/trend/distribution/top）**
4. **Frontend-1：前端骨架 + Layout + Router + API 封装**
5. **Frontend-2：Runs 页面（列表+抽屉详情+新增/编辑含动态 metrics）**
6. **Frontend-3：Dashboard 页面（筛选 + 3图 + TopN + loading/skeleton + 缓存）**

每一步结束你都能：

- 后端 `http://localhost:8080/health` OK
- 前端 `http://localhost:5173` 打开并能看到对应页面

------

# A) 后端 Spring Boot Prompt（建议你直接复制使用）

## 后端 Prompt：Backend-1（项目骨架 + 实体 + 基础 CRUD）

> 发给 AI 的内容如下（原样复制）：

**任务：实现 Spring Boot 3 + MyBatis-Plus + MySQL 后端骨架，完成 project / metric_def / tag 的 CRUD + 分页查询，提供统一响应封装与 health 接口。**

### 约束

- Maven 项目；Java 17；Spring Boot 3
- 依赖：spring-boot-starter-web、validation、mysql-connector-j、mybatis-plus-boot-starter、lombok
- 建表 SQL 放在 `backend/sql/schema.sql`
- 配置在 `application.yml`，端口 8080
- 响应体：`ApiResponse<T> { int code; String message; T data; }`
- 分页响应：`PageResult<T> { List<T> records; long total; long page; long size; }`
- 提供 `GET /health` 返回 ok

### 输出

- 完整目录结构与文件清单（含路径）
- `schema.sql`（project/metric_def/tag 三表即可）
- Entity/Mapper/Service/Controller 代码
- curl 示例验证分页与 CRUD

------

## 后端 Prompt：Backend-2（Run + Metrics + Tags 事务）

> 发给 AI 的内容如下：

**任务：新增 run / run_metric / run_tag 表与对应实体、CRUD，并实现 Run 的创建/更新为事务：创建 run 后批量插入 run_metric 与 run_tag；更新时先删后插。提供以下接口：**

- `GET /api/runs?page&size&projectId&status&q&dateFrom&dateTo&tagIds`
- `GET /api/runs/{id}`（返回 run + tags + metrics（含 metric_def 信息））
- `POST /api/runs`
- `PUT /api/runs/{id}`
- `DELETE /api/runs/{id}`

### 必须遵循的 DTO

- RunCreateUpdateRequest：包含 run 字段 + `tagIds: number[]` + `metrics: {metricDefId:number,value:number}[]`
- RunDetailResponse：包含 tags（id,name）与 metrics（metricDefId,name,displayName,direction,value）

### 技术要求

- 使用 `@Transactional`
- metrics 不能重复 metricDefId（后端校验，重复返回 code!=0）
- run_name 在同一 project 下建议唯一（可选校验）
- list 接口返回 `metricPreview`（acc/loss/f1 三个若存在则返回），实现方式可用子查询或服务层组装（只要稳定即可）

### 输出

- 更新后的 schema.sql（新增 run/run_metric/run_tag）
- 新增/修改文件清单 + 关键代码
- curl 示例：创建 run（含3个指标+2个tag）→ 查询详情 → 更新 → 查询列表

------

## 后端 Prompt：Backend-3（Dashboard 聚合接口，含 T2）

> 发给 AI 的内容如下：

**任务：实现 Dashboard 聚合接口：**

- `GET /api/dashboard/summary`
- `GET /api/dashboard/trend`（Best-of-day：direction=MAX 用 MAX(value)，direction=MIN 用 MIN(value)；按 DATE(run.created_at) 分组）
- `GET /api/dashboard/distribution`（by=model|dataset|tag）
- `GET /api/dashboard/top`（direction 决定 asc/desc）

### 要求

- 聚合查询建议用自定义 `DashboardMapper`（XML 或 @Select），不要强行用 QueryWrapper 写复杂 join/group
- summary 返回：totalRuns、runsLast7Days、successRate、bestMetric（runId,value,metricName）
- trend 返回：[{x:"YYYY-MM-DD", y:number}]
- distribution 返回：[{name:string, value:number}]
- top 返回：[{runId, runName, modelName, datasetName, value, createdAt, status}]
- 所有接口支持过滤：projectId、dateFrom、dateTo、status、tagIds（tagIds 过滤用 EXISTS run_tag）

### 输出

- DashboardMapper 与 SQL
- Controller/Service
- curl 示例验证（给出完整 URL 参数）

------

# B) 前端 Vue3 Prompt（建议你直接复制使用）

## 前端 Prompt：Frontend-1（骨架 + 路由 + Layout + API 封装）

> 发给 AI 的内容如下：

**任务：创建 Vue3 + Vite + Element Plus + Router + Pinia 项目骨架，做一个后台布局：左侧菜单 + 顶部栏 + 内容区。实现 Axios 封装与 API 类型定义。**

### 页面路由

- `/dashboard`
- `/runs`
- `/projects`
- `/about`

### 要求

- Element Plus 全局引入
- Axios baseURL 指向 `http://localhost:8080`
- 统一处理 ApiResponse：code!=0 弹出 ElMessage.error
- 提供 `src/api/types.ts`（ApiResponse/PageResult 等类型）
- 提供 `src/api/*`：projects、runs、dashboard、tags、metricsDefs

### 输出

- 完整目录结构
- Layout 组件（侧边栏菜单）
- 路由配置
- 一个简单的 Dashboard 占位页能打开

------

## 前端 Prompt：Frontend-2（Runs 页面：列表 + 抽屉详情 + 新增/编辑含动态 metrics）

> 发给 AI 的内容如下：

**任务：实现 Runs 页面：**

- 顶部筛选：project、status、关键词 q、日期范围
- 表格：分页、loading、空状态
- 行操作：查看详情（抽屉）、编辑（弹窗）、删除
- 新增按钮：打开弹窗表单

### 关键：Run 表单（Dialog + Form）

- 字段：projectId、runName、status、modelName、datasetName、optimizer、lr、batchSize、epochs、seed、note、tagIds
- Metrics 动态表单：用 el-table 展示 metrics 数组，每行 metricDefId select + value input-number；支持添加/删除行；前端校验不允许重复 metricDefId
- 提交时调用 POST/PUT /api/runs
- 查看详情抽屉：展示 run 基础信息 + tags + metrics 表格

### 数据来源

- projects 下拉：GET /api/projects
- tags 下拉：GET /api/tags
- metric defs 下拉：GET /api/metrics/defs

### 输出

- `src/views/Runs.vue` 及相关组件拆分（若需要）
- 完整可运行，能创建 run（含多指标）并在列表/详情展示

------

## 前端 Prompt：Frontend-3（Dashboard 页面：筛选 + 图表 + TopN + 缓存）

> 发给 AI 的内容如下：

**任务：实现 Dashboard 页面：**

- 筛选：project、日期范围、status、metric（acc/loss/f1）、distributionBy（tag/model/dataset）
- KPI 卡片：totalRuns、runsLast7Days、successRate、bestMetric
- 图表：
  - trend 折线图：调用 `/api/dashboard/trend`，展示 Best-of-day
  - distribution 柱状/饼图：调用 `/api/dashboard/distribution`
  - TopN 表格：调用 `/api/dashboard/top`
- 体验：Skeleton/Loading；筛选变化时请求；同样参数 30 秒内命中前端缓存（Map key=JSON.stringify(params)）

### 输出

- `src/views/Dashboard.vue`
- `src/utils/cache.ts`（简单 TTL 缓存）
- 图表用 ECharts（封装一个 `useEcharts` 或组件）

------

# Step 12.2 Git 提交节奏（保证“过程分”）

每完成一个里程碑就 commit（老师最爱看这个）：

1. `chore: bootstrap backend`
2. `feat: project/tag/metric CRUD`
3. `feat: run CRUD with metrics and tags`
4. `feat: dashboard aggregation apis`
5. `chore: bootstrap frontend layout`
6. `feat: runs page`
7. `feat: dashboard page`
8. `perf: caching and loading states`
9. `docs: add system docs skeleton`

------

# Step 12.3 你现在该怎么开始执行（不写代码也不乱）

建议你现在就进入 **Backend-1**，因为后端 schema + 实体定下来，AI 写前端才不会反复改接口。

你可以把我上面的 **Backend-1 Prompt** 直接发给你的 AI 开发助手。

------

如果你愿意，我也可以把这套 Prompt 进一步“压缩成你项目专用的一份 `AI_DEV_PLAN.md`”，你直接丢给 AI，它按步骤执行并输出每一步的改动清单和 commit 信息。你只要说一句：**“生成 AI_DEV_PLAN.md”**。