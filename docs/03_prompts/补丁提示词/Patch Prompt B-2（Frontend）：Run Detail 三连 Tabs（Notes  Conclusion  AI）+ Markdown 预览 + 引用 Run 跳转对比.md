# Patch Prompt B-2（Frontend）：Run Detail 三连 Tabs（Notes / Conclusion / AI）+ Markdown 预览 + 引用 Run 跳转对比

```
你是资深前端工程师。请为 ExperimentalAssistant 的 Runs 详情抽屉（Run Detail Drawer）增加 Notes / Conclusion / AI 三个 Tab，支持 Markdown 编辑与预览，并支持在 Markdown 中引用 [[run:ID]] 点击跳转打开对应 Run 的详情抽屉。必须保持现有 Runs 列表、新建编辑弹窗、Dashboard 不受影响。

【冻结约束】
- Vue 3 + Vite + Element Plus + Router + Pinia + Axios
- 统一接口响应：code!=0 -> ElMessage.error
- 不做：auth、复杂富文本编辑器（可用 Markdown + 预览即可）、不做实时协作

============================================================
B-2 Goal：Run Notes/Conclusion/AI Panel（UI）
============================================================

A) 依赖建议（允许新增，但要轻）
- 使用 markdown-it 做预览渲染（或等价轻量库）
- 不上重型 WYSIWYG；Markdown 编辑用 textarea + Preview 即可（Split View）
- 为 [[run:123]] 自定义渲染规则：渲染成可点击链接/Tag

B) API 封装（必须）
新增：
- src/api/runNotes.ts
  - listNotes(runId, type?)
  - createNote(runId, payload)
  - updateNote(runId, noteId, payload)
  - deleteNote(runId, noteId)
  - getConclusion(runId)
  - upsertConclusion(runId, payload)
- src/api/ai.ts
  - generateRunDraft(payload) -> AiDraftResponse
- 可选：src/api/markdown.ts 解析 refs

C) UI 位置（必须）
在 Run Detail Drawer 中增加 Tabs（放在 metrics/tags 基础信息之后）：
- Tab1: Notes
- Tab2: Conclusion
- Tab3: AI

D) Notes Tab（必须）
- 列表：按 updatedAt 倒序展示 NOTE + AI_DRAFT（可用筛选开关）
- 每条支持：Edit / Delete
- 新增 Note：按钮打开 Dialog（title 可选，contentMd 必填）
- 编辑采用 Markdown Split View：
  - 左：el-input type=textarea（autosize）
  - 右：Preview（markdown-it 渲染）
- 保存后刷新列表

E) Conclusion Tab（必须）
- 语义：每个 run 只有一个“当前结论”
- 展示：
  - 如果无结论：空状态 + “写结论”按钮
  - 如果有：渲染 Markdown Preview + “编辑”按钮
- 编辑：同 Split View（textarea + preview）
- 额外动作：
  - “从某条 AI_DRAFT 设为结论”：在 Notes Tab 里对 AI_DRAFT 增加按钮 “Set as Conclusion” -> 调用 upsertConclusion

F) AI Tab（必须，但可离线用 Mock）
- UI：
  - 一个可选输入框 userHint（“你想让 AI 关注什么？”）
  - 4 个开关 includeMetrics/includeTags/includeTemplate/includeNotes（默认都 true）
  - 按钮 “生成结论草稿”
- 生成后显示：
  - 草稿 Markdown（Split View：可编辑 + 预览）
  - extracted.keyFindings / nextSteps 用小列表展示（可复制）
- 两个按钮：
  - “保存为 AI 草稿”（createNote type=AI_DRAFT）
  - “写入为结论”（upsertConclusion）
IMPORTANT：任何 AI 结果必须用户点击确认才落库

G) Markdown 引用 Run 跳转（必须）
- 约定：[[run:123]]
- 渲染时把它变成可点击元素（如 <el-link> 或可点击 tag）
- 点击后行为：
  - 在当前页面复用 Run Detail Drawer，加载 runId=123 的详情（相当于“对比跳转”）
- 注意防止无限递归：允许跳转，但 drawer 还是同一个，只是切换当前 runId

H) 验收清单（必须提供）
- 打开某条 run -> Notes 新增/编辑/删除
- AI Tab 生成草稿（即便后端是 mock）-> 保存为 AI_DRAFT -> 一键写入 Conclusion
- Conclusion 渲染 Markdown + [[run:ID]] 跳转打开另一条 run 的详情
- 不选任何新功能也不影响原 Runs 列表与编辑弹窗

I) 输出格式（必须）
- 文件路径变更树
- 关键组件代码（RunDetailDrawer.vue 或对应文件）
- 新增 api 文件代码
- 建议 commit 拆分（至少 2 个）：
  - feat: add run notes and conclusion markdown UI
  - feat: add ai draft panel and run reference navigation

```

