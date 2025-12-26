# Project Page Prompt

下面这份是**完全按你们原来的提示词范式**写的「补丁式 Prompt」，并且**基于你给的实际目录结构**（已经有 `src/api/projects.ts`、`ProjectsView.vue` 等）。你说的前置条件我也遵守：**你会在它之前自动拼接 `AI_CONTEXT.md` 作为第 1 段提示词**，所以下面只写“补丁阶段 prompt”。

> 使用方式：把下面整段作为一个新的阶段（建议命名 `Frontend-Projects`）丢给你的 AI agent。
> 目标：**让 Projects 页面真正可用且好看**，并带上你要的“加分小补丁”（View Runs + Runs Count badge）。

------

## Frontend-Projects Patch Prompt（精美可用 + 加分联动）

### Stage Goal

在不重构现有布局/路由/其他页面的前提下，**把 `ProjectsView.vue` 从占位页升级为“可验收的项目管理模块”**：

- Projects 列表（分页、搜索、刷新、重置）
- Create / Edit / Delete Project（弹窗表单、校验、加载态）
- 关联入口：每行提供 `View Runs` 按钮（跳转 Runs 页面并带过滤参数）
- 加分小补丁：每行显示一个 `Runs Count` 徽标（若后端不支持则自动降级隐藏，不允许报错）

### Repo Reality（必须遵循现有结构，禁止“重新搭架子”）

你必须基于以下实际文件结构开发（不要新建不必要目录）：

- `src/views/ProjectsView.vue` ✅ 主要实现目标
- `src/api/projects.ts` ✅ 已存在：必须复用/补全
- `src/api/runs.ts` ✅ 已存在：用于 runs count / 跳转联动
- `src/api/types.ts` ✅ 已存在：若已有分页/Result类型，必须复用
- `src/views/RunsView.vue` ✅ 可允许极小改动：仅用于读取 query 参数并自动预填筛选（禁止重构）
- 其他页面（Dashboard/About/App/layout/router/utils）禁止大改

### Hard Constraints（不可违反）

1. 技术栈：Vue3 + Element Plus，保持与你们现有页面一致的 UI 风格（留白、卡片、浅灰背景、表格风格统一）。
2. API 调用：必须使用项目现有的请求封装（在 `src/utils` 里），并遵循统一错误处理（`code != 0 -> ElMessage.error(message)`）。
3. 分页：Projects 列表必须分页请求，严禁一次拉全量。
4. **补丁式开发**：不允许“重写 Projects 页面结构到新的组件体系”，只在当前项目框架内最小改动实现高完成度。
5. `Runs Count` 必须“best-effort”：如果后端/接口字段不支持，必须自动降级（不展示/显示 `-`），不得影响 Projects 页面主流程。

------

## UI / UX Spec（要“精美”，不是能用就行）

### Layout

`ProjectsView.vue` 采用与 Dashboard/Runs 一致的结构：

- 顶部：Title（Projects）+ Subtitle（Manage your experiment projects.）
- 主卡片：工具栏（搜索/按钮）+ 表格 + 分页
- Empty State：无数据时给引导文案 + Create Project 按钮（不要一片空白）

### Toolbar

必须包含：

- 搜索框：placeholder “Search by project name”，支持 Enter 触发
- 按钮：Filter / Reset / Refresh / Create Project（Create 为主按钮）
- 交互细节：Reset 会清空搜索并回到第一页，Refresh 不改筛选条件仅重拉

### Table Columns（建议固定，做出质感）

- **Name**（加粗，可点击，点击等同 View Runs）
- Description（最多两行，溢出省略）
- **Runs**（加分列：徽标 badge，显示 run 数）
- Created At（格式化展示）
- Actions：Edit / Delete / View Runs（View Runs 也可以放在 Actions 里）

### Create/Edit Dialog

- 使用同一个弹窗组件逻辑（在同文件或抽小组件都行，但不要过度工程化）
- 字段：
  - name：必填、长度 2~50
  - description：可选、长度 <= 200，textarea
- 提交按钮有 loading
- 成功后：关闭弹窗 + 刷新列表 + success toast

### Delete

- ElMessageBox.confirm 二次确认
- 成功后：刷新列表（若当前页空了需回退一页再拉取）

------

## API Integration（必须 work）

### Projects API（复用 `src/api/projects.ts`）

确保以下方法存在并可用（如已有则适配签名与返回类型）：

- `listProjects(params: { page:number; size:number; q?:string })`
- `createProject(payload: { name:string; description?:string })`
- `updateProject(id:number|string, payload: { name:string; description?:string })`
- `deleteProject(id:number|string)`

### Runs Count（加分小补丁，必须“安全降级”）

利用 `src/api/runs.ts` 实现每个 project 的 run 数显示策略：

**策略 A（推荐，后端常见分页返回 total）：**
调用 runs 列表接口按 projectId 过滤并只取一页（`size=1`），从响应 `total` 获取数量。

- 例如：`listRuns({ page:1, size:1, projectId: project.id }) -> total`
- 要求：并发限制（避免一次性对每行发请求把后端打爆）
- 触发时机：Projects 列表加载后 **延迟/批量** 拉取当前页项目的 count（例如 `Promise.allSettled` + 并发池 3~5）

**策略 B（若后端无 total 或不支持 projectId 过滤）：**
Runs 列显示 `-` 或直接隐藏该列（由代码自动判断），不得报错，不得影响 Projects CRUD 主流程。

> 你必须在代码里写清楚“如何判断支持 total / projectId”，并在控制台不输出红色 error。

------

## Runs 联动（必须有，且改动极小）

### Projects -> Runs 跳转

实现以下跳转方式之一（优先用 id）：

- `router.push({ name:'Runs', query: { projectId: String(project.id) } })`
  或（如果后端 Runs 用 projectName 搜索）：
- `query: { project: project.name }`

### RunsView.vue 最小补丁（可选但强烈建议，属于加分）

在 `RunsView.vue` 里读取 query 参数并预填筛选：

- 页面 mounted 时：
  - 如果 `route.query.projectId` 存在：设置筛选条件并自动触发 Filter 拉取
- 限制：只允许增加很小一段逻辑，不得重构整个 Runs 页面状态管理

------

## Edge Cases（必须处理，不然“看起来不专业”）

1. 搜索后分页：切换页码时保留 q 条件
2. Reset：清空条件回到第 1 页并重新加载
3. 删除最后一条：若当前页变空，自动 page-- 后再拉取
4. 弹窗重复打开：表单必须 reset（不要残留上一次数据）
5. Runs Count：接口失败要吞掉（`allSettled`），展示 `-`，不得 toast 打扰用户

------

## Deliverables（必须产出这些文件改动）

1. ✅ `src/views/ProjectsView.vue` 完整实现（含 CRUD、联动、runs count badge、空态、loading、分页）
2. ✅ `src/api/projects.ts`：确保 CRUD + list 参数与返回类型严谨（复用 `src/api/types.ts`）
3. ✅（可选加分）`src/views/RunsView.vue`：最小补丁支持从 query 预筛选
4. ✅ 若需要，补充 `src/api/types.ts` 的类型（但不能破坏现有 import）

------

## Suggested Commits（必须按阶段提交，便于验收）

1. `feat: projects api functions and types`
2. `feat: projects page list search pagination empty state`
3. `feat: projects create edit delete dialogs`
4. `feat: projects link to runs and runs-count badge (best-effort)`
5. （可选）`feat: runs page read project filter from route query`

------

## Manual Test Checklist（你写完必须自测通过）

- Projects 页面不再空白：有工具栏、表格、分页、空态引导
- Create Project 成功后能看到新增项目
- Search/Reset/Refresh 正常
- Edit 能更新列表
- Delete 有确认，且删除后分页不会卡死
- 点击 Name 或 View Runs 能跳到 Runs 页面，并带筛选参数
- Runs Count 在支持时显示 badge；不支持时显示 `-` 或隐藏，不报错不影响主功能

------

## Output Requirements（AI 输出必须包含）

- 具体改了哪些文件（文件路径列表）
- 每个文件的关键代码片段（可直接复制粘贴）
- 若对 `RunsView.vue` 做了补丁，必须说明“为什么必要 + 改动范围多小”
- 说明 Runs Count 的降级逻辑（支持/不支持时如何表现）

------

### 额外提醒（你们这个项目会因此更像“成熟产品”）

Projects 页做完后，Dashboard 的 KPI（Total Runs / Best Metric 等）才有“数据闭环”的意义，这对老师“完整性/工作量”评分非常关键。

------

如果你愿意，我还能给你再加一个**不改后端也能加分**的小点：
在 Projects 表格里加一个 `Quick Create Run` 的按钮（点击跳到 Runs 页并打开 Create Run 弹窗/或带 query `?create=1`），但这要看你 `RunsView.vue` 有没有现成的 Create Run 弹窗逻辑。你把 `RunsView.vue` 里“Create Run”相关代码截一小段给我，我就能把这个加分点也写进 patch prompt（仍然保持补丁式、改动极小）。