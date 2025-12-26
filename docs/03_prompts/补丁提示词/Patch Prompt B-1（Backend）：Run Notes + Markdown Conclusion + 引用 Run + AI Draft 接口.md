# Patch Prompt B-1（Backend）：Run Notes + Markdown Conclusion + 引用 Run + AI Draft 接口

你是资深后端工程师。请在不引入权限、WebSocket、MQ、真实训练逻辑的前提下，为 ExperimentalAssistant 增加“Run 笔记/结论（Markdown）+ 引用 run_id 对比 + AI 草稿接口（可插拔）”能力。必须保持现有 Runs/Dashboard 功能与 API 不破坏，新增均为可选增强。

【冻结约束】
- Spring Boot 3 + MyBatis-Plus + MySQL，Java 17，Maven
- 返回统一：{ code, message, data }
- 分页：{ records, total, page, size }
- 不做：auth/rbac、WebSocket、MQ、CSV 导入、真实训练/模型逻辑
- 现有表：project, run, metric_def, run_metric, tag, run_tag（不删不改既有字段语义）
- Run create/update 事务一致性保持（run + run_metric + run_tag）

============================================================
B-1 Goal：Run Notes / Conclusion（Markdown）/ AI Draft（可插拔）
============================================================

A) 数据库（必须）
在 backend/sql/schema.sql 增加一张新表（不要破坏旧表初始化能力）：

1) run_note
- id BIGINT PK
- run_id BIGINT NOT NULL
- type VARCHAR(16) NOT NULL   # NOTE | CONCLUSION | AI_DRAFT
- title VARCHAR(128) NULL     # 可选，用于列表显示
- content_md MEDIUMTEXT NOT NULL  # Markdown 内容
- created_at DATETIME
- updated_at DATETIME
索引与约束：
- INDEX(run_id)
- 可选：每个 run 只允许 1 条 CONCLUSION（用业务逻辑保证，不强制唯一索引，避免迁移复杂）

B) 后端结构（必须）
新增模块：
- entity: RunNote
- dto:
  - RunNoteCreateRequest { type, title, contentMd }
  - RunNoteUpdateRequest { title, contentMd }
  - RunNoteResponse { id, runId, type, title, contentMd, createdAt, updatedAt }
  - RunConclusionResponse { runId, conclusion: RunNoteResponse|null }
  - AiDraftRequest { runId, userHint?:string, includeMetrics?:boolean, includeTags?:boolean, includeTemplate?:boolean, includeNotes?:boolean, mode?:string }
  - AiDraftResponse { draftMd:string, extracted:{ keyFindings:string[], nextSteps:string[] } }
- controller/service/mapper：RunNoteController/Service/Mapper
- 新增一个 AiAnalysisController + service（可插拔 provider）

C) 新增 API（必须）
1) Notes CRUD
- GET    /api/runs/{runId}/notes?type=NOTE|AI_DRAFT (可选 type 过滤)
- POST   /api/runs/{runId}/notes
- PUT    /api/runs/{runId}/notes/{noteId}
- DELETE /api/runs/{runId}/notes/{noteId}

2) Conclusion（单条语义）
- GET  /api/runs/{runId}/conclusion
- PUT  /api/runs/{runId}/conclusion
  请求体：{ title?, contentMd }
  行为：若已有 CONCLUSION 则更新；否则创建 type=CONCLUSION

3) AI Draft（必须可用但允许 mock）
- POST /api/ai/run-draft
  请求体：AiDraftRequest
  行为：
  - 拼接上下文（由 include* 控制）：
    - run 基础字段
    - metrics（含 direction）
    - tags
    - template 信息（如果 run 有 templateId，templateName 可选；若系统已有 templates 模块则读取，否则忽略）
    - 最近 N 条 NOTE（建议 N=5）
  - 调用 AiProvider 生成 draftMd（Markdown），并返回 extracted 结构化建议
  - IMPORTANT：默认不自动写入数据库；由前端“确认写入”时再调用 PUT /conclusion
  - 可选增强：提供一个 query 参数 saveDraft=true 时，把 draft 保存为 run_note(type=AI_DRAFT)

D) AiProvider 设计（必须，保证可插拔、默认可跑）
- interface AiProvider { AiDraftResponse generate(AiContext ctx); }
- 默认实现 MockAiProvider：
  - 不调用外网
  - 根据 metrics 的 best/worst、status、notes 里关键词（"oom","nan","lr","overfit"）生成一个“像样”的 Markdown 草稿
- 可选实现 HttpAiProvider：
  - 读取 application.yml 配置：ai.baseUrl, ai.apiKey, ai.model
  - 用 RestTemplate/WebClient 调用外部 AI（仅提供骨架，不要写死某厂商 SDK）
  - 当未配置时自动回退 MockAiProvider

E) 引用 run_id 机制（后端仅提供“解析辅助”即可，必须）
- 约定 Markdown 中 run 引用语法：[[run:123]]
- 后端提供一个轻量解析接口（可选但推荐）：
  - POST /api/markdown/refs
    请求：{ textMd }
    响应：{ runIds:number[] }  # 正则提取 [[run:ID]]
    前端可不用这个接口也行，但有则更稳

F) 验收（必须提供 curl）
- 创建/更新结论（PUT /conclusion）
- 新增 note（POST /notes）
- 列表 notes（GET /notes）
- 生成 AI 草稿（POST /api/ai/run-draft）
- 说明：不要求接真实 AI，MockAiProvider 返回即可

G) 输出格式（必须）
- 文件变更路径树
- 关键代码可复制粘贴（controller/service/mapper + MockAiProvider）
- 启动方式
- 建议 commit 拆分（至少 2 个）：
  - feat: add run notes and conclusion markdown apis
  - feat: add ai draft endpoint with pluggable provider