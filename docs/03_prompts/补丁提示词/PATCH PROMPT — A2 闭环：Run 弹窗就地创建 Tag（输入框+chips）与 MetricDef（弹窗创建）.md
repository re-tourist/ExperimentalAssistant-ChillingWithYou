# PATCH PROMPT — A2 闭环：Run 弹窗就地创建 Tag（输入框+chips）与 MetricDef（弹窗创建）

你将基于现有项目代码，在**不新增页面、不引入鉴权、不破坏现有 IA 与 API 约定**的前提下，完成一个关键闭环修复：

- 当前最大问题：Run 创建/编辑弹窗中 Tag 与 Metric 下拉显示 “No data”，导致用户无法创建自己的 Tag 与 MetricDef，无法录入 Run 的指标与标签。
- 本补丁目标：在 Run 弹窗内实现“就地创建”：
  - Tag：做成 **输入框 + chips**（已选标签以 chip 展示），用户在输入框中输入 Tag 名称，按 Enter 即创建（若不存在）并添加为已选；若已存在则直接选中添加；同时支持从 chips 移除已选。
  - MetricDef：在 metrics 区域支持“新增指标定义”弹窗（name/displayName/direction），创建后自动加入下拉并选中，用户即可填写 value。
- 本次只完成 A2 闭环，不做 A1 字典管理页，不做登录注册。

---

## 0. 冻结约束（必须遵守）
- 不新增页面/路由（不增加 Tags 页、MetricDefs 页）。
- 不引入登录/鉴权/RBAC，不新增权限框架。
- 不新增 CSV 导入、实时通信、工作流等功能。
- 保持统一响应结构 `{code,message,data}`；分页 `{records,total,page,size}`。
- 数据库遵循既定 schema（project/run/metric_def/run_metric/tag/run_tag）。
- Run create/update 必须事务：run + run_metric + run_tag（现有实现若已是事务，不要破坏）。

---

## 1. 你必须先做的“现状判定”（务必自动分析，不要问我）
### 1.1 后端接口现状检查（必须做）
在后端代码中搜索以下内容，确认是否已存在 Tags / MetricDef 的接口与 service：
- Controller 路由：`/api/tags`、`/api/metric-defs`（或类似命名）
- Mapper/Service：Tag、MetricDef 的 list/create/update/delete 方法
- Runs 获取详情接口是否已返回 tags 与 metricDefs（或是否需要前端额外拉取）

输出一段“判定结果”写入你的 PR 描述或提交说明（简短即可）：
- ✅ 已有：列出已有接口路径与字段
- ❌ 缺失：列出缺失项，并按下面“最小补齐”实现

### 1.2 前端现状检查（必须做）
在前端查找 Run 创建/编辑弹窗组件（通常在 RunsView 下的 Dialog 组件）：
- Tag 当前是 Select 还是别的？
- Metric 当前如何渲染（动态列表？每行一个 metricDef + value）？
- tags 与 metricDefs 的数据源来自哪里（页面初始化拉取？弹窗打开拉取？）

---

## 2. 需要达成的最终交互（验收口径）
### 2.1 Tag 输入框 + chips（高级交互，必须实现）
在 Run Dialog 的 Tags 区域：
- 显示已选 tags：使用 Element Plus 的 `<el-tag>` 或 `<el-tag closable>` 形成 chips。
- 下方/旁边提供一个输入框（`<el-input>`）：
  - placeholder: `输入标签，回车添加`
  - 用户输入后按 Enter：
    1) `trim()` 输入
    2) 空字符串不处理
    3) 先在本地 tags 列表中做 case-insensitive 匹配（建议统一转小写比较，但展示保留原始 name 或统一规范）
    4) 若已存在 tag：将其添加到“已选 tagIds”（若未选中）
    5) 若不存在：调用创建接口创建 tag（POST），创建成功后：
       - 将新 tag push 到 tags 列表
       - 自动选中（加入 tagIds）
    6) 清空输入框，保持焦点（加分）
- chips 的删除：点击 chip 的 close，将 tagId 从已选中移除（仅解除关联，不删除 tag）
- 避免重复添加：同一个 tag 不能重复出现在已选 chips。
- 若创建失败且原因是“已存在”：自动选择已存在项（体验加分）。如果后端没有做这种返回，就前端在创建失败后重新 fetch tags 并尝试匹配再选中。

> 注意：本次不做“编辑/删除 tag 资源本身”的功能，只做选择/创建/解除选择。

### 2.2 MetricDef 就地创建（必须实现）
在 Run Dialog 的 Metrics 区域：
- 每行 metric 结构：metricDefId + value
- “选择指标定义”的下拉若无数据，不要显示 “No data” 结束，而是显示引导：
  - 文案：`暂无指标定义，请先创建`
  - 提供按钮：`+ New Metric`
- 点击 `+ New Metric` 打开一个小弹窗（Element Plus Dialog）：
  - 表单字段：
    - name（必填，建议小写英文+下划线，长度<=32）
    - displayName（必填，长度<=64）
    - direction（必选：MAX/MIN；默认 MAX）
  - 提交成功后：
    - 刷新 metricDef 列表（或将返回对象插入列表）
    - 自动在当前正在编辑的那一行 metric 中选中新建的 metricDefId
    - 关闭弹窗，回到指标 value 输入（加分：自动 focus 到 value）
- 同一个 Run 内禁止重复选择同一个 metricDefId（前端校验 + 后端唯一约束兜底）。

---

## 3. 必要的后端“最小补齐”规范（如果你判定缺失就实现）
> 只要满足 A2 闭环，最小需要：**list + create**。update/delete 可先不做（可保留后门但不暴露 UI）。

### 3.1 Tags API（最小）
- GET `/api/tags`
  - 返回全部 tags（无需分页即可；如果你们已有分页也可以，但前端至少要拿到全量用于匹配）
- POST `/api/tags`
  - body: `{ name: string }`
  - 规则：
    - name trim
    - length 1~32
    - UNIQUE（case-insensitive 优先；至少数据库 UNIQUE）
  - 返回创建后的 tag（含 id, name）

建议实现一个“已存在则返回已有”的友好行为（二选一）：
- A：返回 code=0 data=existingTag（最顺滑）
- B：返回 code!=0 message="Tag already exists"（前端需 catch 并重拉）

### 3.2 MetricDef API（最小）
- GET `/api/metric-defs`
  - 返回全部 metric defs
- POST `/api/metric-defs`
  - body: `{ name, displayName, direction }`
  - 规则：
    - name UNIQUE
    - direction in {MAX, MIN}
  - 返回创建后的 metricDef

### 3.3 DB 与约束
- tag.name UNIQUE
- metric_def.name UNIQUE
- run_metric UNIQUE(run_id, metric_def_id)（已有则保持）
- run_tag UNIQUE(run_id, tag_id)

如果你发现后端 DB 未建表或缺字段，不要扩表做复杂设计；仅按 DB_SCHEMA 补齐，并在迁移脚本/初始化 SQL 中加上必要 UNIQUE 约束。

---

## 4. 前端实现要求（具体到状态与容错）
### 4.1 数据加载策略（避免 No data）
当 Run Dialog 打开时：
- 并行加载：
  - tags 列表（用于输入匹配与 chips 显示）
  - metricDefs 列表（用于 metrics 下拉）
- 显示最小 loading（按钮 disabled 或 input loading 均可）
- 若加载失败：显示 ElMessage 错误提示，且保持弹窗可关闭

### 4.2 表单提交结构（不要改现有 Runs API 结构）
创建/更新 Run 时请求体必须包含：
- 基础信息
- `tagIds: number[]`
- `metrics: Array<{ metricDefId: number, value: number }>`
保持与后端约定一致。

### 4.3 输入规范化（必须）
- Tag 输入：trim；空不提交；长度限制；可选：统一转小写存储（若你们后端做 case-insensitive unique）
- Metric name：trim；建议只允许 `[a-z0-9_]+`（不强制也可，但要提示）

---

## 5. 验收测试（你必须自测并在 PR 描述里勾选）
### Tag（P0）
- [ ] Run Dialog 打开后 tags 能加载（不再是 No data 阻断）
- [ ] 输入新 tag 按 Enter：能创建并立刻出现在 chips
- [ ] 输入已存在 tag 按 Enter：不会重复创建，直接选中加入 chips
- [ ] chips 可移除（只解除关联，不删除 tag）
- [ ] 同一 tag 不会重复出现在 chips

### MetricDef（P0）
- [ ] metricDefs 为空时有引导与 `+ New Metric` 按钮
- [ ] 新建 MetricDef 成功后，下拉可选且自动选中
- [ ] 同一 Run 内禁止重复指标定义
- [ ] 创建/更新 Run 成功后，详情页能看到 tags 与 metrics

### 回归（P0）
- [ ] Runs 列表、详情抽屉、编辑、删除均不受影响
- [ ] Dashboard 不崩溃（即使数据少也要空态友好）

---

## 6. 交付物要求（必须输出）
- 代码实现（前后端视情况最小补齐）
- 更新/新增的手工测试记录：在 `manual_test_checklist_runs.md` 末尾追加一段 A2 相关测试 run（简要记录步骤与结果）
- PR 描述包含：
  - 现状判定（后端是否已有 tags/metric-defs CRUD）
  - 本补丁做了什么
  - 自测 checklist（第 5 节勾选）

---

## 7. 质量与风格（必须）
- UI 文案不要出现冷冰冰的 “No data” 作为终点，要给出下一步操作。
- 错误提示要让用户知道怎么解决（例如“标签已存在，已为你选择该标签”）。
- 代码风格保持项目一致（命名、目录、axios 封装、Result 结构等）。
- 禁止引入大依赖；能用 Element Plus 原生组件完成。

现在开始执行：先做现状判定，然后按最小补齐实现 A2 闭环，最后补齐自测记录与 PR 描述。