## 专项开发 Prompt：Run 的非核心字段改为 Template 动态配置（替代固定的 Model/Dataset/Optimizer/Epoch…）

### 0) 目标一句话

把 Create Run 弹窗（图1）里**除 Project/Status/Run Name 外的字段**（如 Model/Dataset/Optimizer/Learning Rate/Batch Size/Epochs/Seed/Note 等）从“写死的深度学习表单”改为**由 Project 所选 Template 的字段定义动态渲染**，从而支持物理/化学/工程等任意领域实验记录。

------

# 1) 现状功能分析（必须写入 PR 描述）

## 1.1 图1当前固定字段的“真实交互特性”

你需要在现有 Create Run 组件里确认这些字段的 UI/校验行为（不要凭感觉，读代码）：

- **Project**：Select，下拉选项目（必填）
- **Status**：Select（必填，默认 RUNNING）
- **Run Name**：Input（必填）
- **Tags**：输入框 + 回车添加（chips），回车逻辑要 `preventDefault`（否则会刷新/submit）
- **Model**：Input（自由输入）
- **Dataset**：Input（自由输入）
- **Optimizer**：Input（自由输入）
- **Learning Rate**：数字输入 + “- / +”调节（浮点，默认 0.001，步长可能是 0.001 或更小）
- **Batch Size**：数字输入 + “- / +”（整数，默认 32，步长 1）
- **Epochs**：数字输入 + “- / +”（整数，默认 10，步长 1）
- **Seed**：数字输入 + “- / +”（整数，默认 42，步长 1）
- **Note**：Textarea（自由输入，可能是 run 的通用字段）

> 结论：这些“深度学习字段”里，**Optimizer 是自由文本**，**Epochs/BatchSize/Seed 必须是整数**且带 stepper，“Learning Rate 是浮点 stepper”。这正是 Template 字段类型与约束要表达的内容。

------

# 2) 新设计：Core Run Fields vs Template Fields（必须严格区分）

## 2.1 Core Run Fields（所有 Run 固有，不可被 Template 取代）

- `projectId`（必选）
- `status`（必选）
- `name`（Run Name，必填）
- `tags`（建议通用保留）
- `note`（是否通用：**建议保留为通用**，但如果你决定让 note 也模板化，要在此明确）

> **硬要求**：UI 上 Project + Status 永远存在且必填；Run Name 永远存在且必填。
> 其他字段（包括 Model/Dataset/Optimizer/Epoch…）全部迁移为 Template Fields。

## 2.2 Template Fields（可配置、可增减、可换类型）

把 Model/Dataset/Optimizer/LR/Batch/Epoch/Seed 等都视为“模板字段”，由模板定义决定是否出现、类型、默认值、约束、UI 控件。

------

# 3) 数据结构与接口改造（后端必须支持动态字段存储）

> 你的 agent 必须先做一个“最小可扩展但不折腾”的持久化方案。推荐优先级如下（从易到难）：

## 方案 A（推荐，最适合课程项目）：Run 表增加 `props_json`（JSON）存动态字段

- `run` 表新增：`props_json JSON NULL`（或 TEXT 存 JSON 字符串）
- Create/Update Run 接口 payload 增加：`props: Record<string, any>`
- 后端校验：根据 Template Field 定义对 `props` 做类型/必填/范围校验
- 好处：改表少、读写简单、支持任意字段集合
- 代价：复杂查询（按某个动态字段筛选）会弱一些，但你当前核心需求不是复杂检索

## 方案 B（更规范）：EAV 表 `run_field_value`

- `template_field` 定义字段，`run_field_value` 存值
- 好处：可查询性强
- 代价：实现量更大（不建议本次专项做）

> **本 prompt 默认你用方案 A**。若你选方案 B，必须在 PR 描述里说明原因与实现范围。

------

# 4) Template 需要支持字段定义（“把图1那些控件描述出来”）

## 4.1 Template 字段定义结构（后端 DTO / DB 表 / 前端编辑用）

为每个 Template 增加 fields（字段定义列表），至少包含：

- `key`：字段唯一键（如 `model`, `dataset`, `epochs`）

- `label`：展示名（如 “Model”/“温度”）

- `type`：`TEXT | TEXTAREA | INT | FLOAT | SELECT | BOOL`

- `required`：是否必填

- `defaultValue`：默认值（用于 Create Run 初始化）

- `placeholder`：占位

- `order`：排序

- `constraints`（JSON）：

  - 对 INT/FLOAT：`min`, `max`, `step`, `precision`（precision 可选）
  - 对 SELECT：`options: string[]`
  - 对 TEXT：`maxLength`（可选）

- `ui`（可选，但建议）：`widget: input | textarea | numberStepper | select | switch`

  > numberStepper 用于实现图1那种 “- 数值 +”

## 4.2 Default Template（用于兼容现有深度学习场景）

Default Template 的字段定义必须能复刻图1的体验（这能证明系统兼容性）：

- model: TEXT（自由输入）
- dataset: TEXT
- optimizer: TEXT
- learning_rate: FLOAT（default 0.001，step 0.001 或 0.0001，显示 stepper）
- batch_size: INT（default 32，step 1，stepper）
- epochs: INT（default 10，step 1，stepper）
- seed: INT（default 42，step 1，stepper）

> 这些只是 default template 的字段，不是系统强制字段。物理模板可以换成温度/压强/浓度/仪器编号等。

------

# 5) Project 创建/编辑：绑定 Template（否则 Run 无法知道渲染什么）

## 5.1 项目必须有 templateId

- `project.template_id`（必有；创建 project 时默认选择 Default Template）
- 如果你们已经有 template 但项目没绑定：补字段与绑定逻辑

## 5.2 Create Run 弹窗里 Project 切换时动态刷新字段

- 当用户在 Create Run 弹窗里选择不同 Project：
  1. 拉取该 project 的 templateId
  2. 拉取 template 的 fields 列表
  3. **用 defaultValue 初始化 props**（注意：用户已输入的值是否保留？建议：切换 project 时清空动态字段，避免误提交）
  4. 重新渲染动态字段区

------

# 6) 前端：Create Run 弹窗 UI 改造（核心）

## 6.1 组件结构建议（必须达到以下行为）

把 Create Run 表单拆成两块：

### A) Core 区（永远固定）

- Project Select（必填）
- Status Select（必填）
- Run Name（必填）
- Tags（chips + 回车添加；回车必须 preventDefault）
- （可选）Note（如果决定是 core）

### B) Template Fields 区（动态渲染）

- 渲染规则：
  - 按 `order` 排序
  - TEXT -> 普通 Input
  - TEXTAREA -> Textarea（占整行）
  - INT -> 数字输入 + stepper（“- / +”，步长默认为 1；禁止小数）
  - FLOAT -> 数字输入 + stepper（允许小数；precision/step 来自 constraints）
  - SELECT -> 下拉框
  - BOOL -> switch/checkbox
- 校验规则：
  - required 的字段必须校验（空值提示：`{label} is required`）
  - INT：必须是整数；输入非整数时提示并阻止提交
  - FLOAT：允许小数，但要符合 min/max（若配置）
- 布局要求（必须解决你说的“字段数量/类型自定义导致排版问题”）：
  - 使用响应式 grid：默认 2 列；屏幕窄则 1 列
  - TEXTAREA / 超长控件占整行
  - 字段很多时弹窗内容区可滚动，不允许溢出遮挡 Confirm 按钮

## 6.2 交互细节（对齐图1体验）

- INT/FLOAT 的 stepper：
  - “-”不会把值减到低于 min（若有 min），否则可减到 0 或负数（按模板决定）
  - “+”不会超过 max（若有）
  - step 默认：INT=1；FLOAT=constraints.step（如 0.001）
  - FLOAT 显示精度：constraints.precision（如果没有就不强制格式化）
- 输入框默认值来自 Template defaultValue（Create Run 打开时即填好）
- 提交时 payload 结构明确（见第 7 节）

------

# 7) API / Payload 约定（必须写清楚，避免 agent 做一半）

## 7.1 Create Run payload（推荐）

```json
{
  "projectId": 1,
  "name": "exp-001",
  "status": "RUNNING",
  "tagIds": [1,2],
  "note": "...", 
  "props": {
    "model": "ResNet50",
    "epochs": 10,
    "learning_rate": 0.001
  },
  "metrics": [
    {"metricDefId": 3, "value": 0.93}
  ]
}
```

## 7.2 后端校验逻辑（必须）

- 读取 project.templateId -> template.fields
- 对 props 做：
  - 只允许 template 定义过的 key（陌生 key 丢弃或报错：二选一，推荐报错以防脏数据）
  - required 校验
  - 类型校验（INT/FLOAT/BOOL）
  - min/max 校验（如果配置了 constraints）
- 保存：
  - core 字段入 run 表
  - props 写入 `run.props_json`

------

# 8) 兼容策略（必须，不然老数据会炸）

你必须在 agent prompt 里要求实现“老 run 兼容”至少做到不崩：

- 如果历史 run 仍有旧列（model/dataset/epochs…）：
  - 读取 run 时：把旧列映射进 props（仅当 props_json 为空时）
  - 或者写一次迁移脚本把旧列写入 props_json
- Create Run 新逻辑不再依赖旧列
- Runs 列表/详情展示如果原来展示 model/dataset：
  - 改成从 props 取（key 来自模板；Default Template 才会有 model/dataset）

> 这一点是 coder agent 最容易漏掉的坑：**UI 改完了，但列表/筛选/详情还在读旧字段**。

------

# 9) 验收清单（必须逐条自测并写 PR 描述）

## 9.1 Default Template 兼容深度学习（图1回归）

-  创建 project 默认绑定 Default Template
-  Create Run 弹窗里仍能看到 “Model/Dataset/Optimizer/LR/Batch/Epoch/Seed”（但它们来自模板，不是写死）
-  Epoch/Batch/Seed 为整数 stepper；LR 为浮点 stepper；Optimizer 为自由文本
-  提交成功后 run.props_json 有对应键值

## 9.2 新模板支持物理/化学字段

-  新建一个 Template：字段为 温度(INT), 压强(FLOAT), 催化剂(TEXT), 备注(TEXTAREA)
-  新建 project 选择该模板
-  Create Run 弹窗不再出现 Model/Dataset/Optimizer 等 DL 字段，改为模板字段
-  校验生效（必填/整数/范围）
-  保存后 props_json 正确

## 9.3 Project 切换联动

-  在 Create Run 弹窗中切换 Project，动态字段区立即刷新为新模板字段
-  不出现旧字段残留或校验错乱

------

# 10) 实施步骤（让 agent 不跑偏）

1. 在前端定位 Create Run 弹窗组件
   - 搜索关键字：`Create Run` / `Run Name` / `learning rate` / `batch size` / `epochs`
2. 抽离 “DL 固定字段” 为 `DynamicFieldsRenderer`（或等价组件）
3. 在后端：
   - run 表新增 `props_json`
   - 项目表新增 `template_id`（若缺）
   - 新增/完善 Template fields API：`GET /api/templates/{id}` 返回 fields
4. 前端在 Create Run 打开时：
   - 根据 projectId 取 template -> fields -> 初始化 props
   - 动态渲染字段并校验
5. 补老数据兼容（读旧列映射或迁移脚本）
6. 全量回归（尤其 Runs 列表/详情展示）

------

# 11) Commit 拆分建议（至少 4 个）

1. `feat: add run props_json for template-defined fields`
2. `feat: add template fields definitions and project-template binding`
3. `refactor: render create-run dynamic fields by template`
4. `fix: backward compatibility for legacy run fields`

------

\##（可选）你仓库里 agent 需要自行确认的“确定性信息”Checklist

> 不问你，让 agent 自己在代码里查到再实现：

- 前端技术栈：Vue+ElementPlus 还是 React+Antd？（决定 InputNumber/Stepper 写法）
- run 当前存储结构：是否已有 model/dataset 等列？（决定迁移方式）
- template 当前是否已存在？是否仅支持预置 metrics？（决定扩展范围）
- Runs 列表/筛选是否依赖 model/dataset 查询？（决定是否需要同步改成从 props 读取）

------

如果你希望我把这份 prompt **进一步落到你项目的具体文件路径与字段名**，你只要把以下两段信息贴我（任意一种即可，不需要全贴）：

1. Create Run 弹窗所在的组件文件名/路径（或你随便贴一段包含 `learning rate` 的代码片段）
2. 后端 Run entity / controller 的字段定义片段（含 model/dataset/epochs 等）

我就能把 prompt 变成“逐文件修改指令 + 关键代码骨架”，agent 基本照抄就能做完。