## Patch Prompt B-3（可选增强，插件化方向）：把“AI 分析”定义为可替换 Provider（不绑定厂商）

> 这一段如果你们想“接入 AI agent API”但又担心不稳定/不想绑定 OpenAI/某厂商，就用它。它不会改变 B-1/B-2 的 UI/数据结构，只是让后端 provider 更像“插件”。

在现有 B-1 的基础上，把 AI Draft 生成做成“Provider 插件机制（轻量）”，并增加最小的可观测性与降级策略。

要求：
1) application.yml 增加：
- ai.provider = mock | http
- ai.http.baseUrl
- ai.http.apiKey
- ai.http.timeoutMs
- ai.http.model (可选)
2) Provider 选择逻辑：
- 若 ai.provider=http 且 baseUrl/apiKey 配齐 -> 用 HttpAiProvider
- 否则回退 MockAiProvider，并在日志 warn
3) HttpAiProvider 协议（不要写死厂商）：
- POST {baseUrl}/generate
- 请求：{ prompt:string, context:any }
- 响应：{ draftMd:string, extracted:{keyFindings:string[], nextSteps:string[]} }
4) 增加简单的请求日志与耗时统计（log + ms），失败回退 mock
5) 不新增权限，不新增队列，不做异步任务

输出：
- 关键配置、provider 代码
- curl 示例：演示未配置时走 mock；配置后走 http（可以用本地 mock server 占位）
- 建议 commit：feat: make ai draft provider pluggable with safe fallback