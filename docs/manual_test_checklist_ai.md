# 手工测试清单 — AI 页面与功能

## 1. 基础页面加载
- [ ] 访问 `/ai` 路由，页面正常加载
- [ ] 左侧边栏显示 "Runs" 和 "Notes" 折叠面板
- [ ] 中间聊天区域显示欢迎页/空状态
- [ ] 底部输入框与发送按钮正常显示

## 2. 虚拟文件树 (Sidebar)
### Runs
- [ ] 默认加载 Runs 列表
- [ ] 搜索框输入关键词，Runs 列表实时/防抖过滤
- [ ] 列表为空时显示 "No runs found"
- [ ] Run 条目显示 Name, ID, Status

### Notes
- [ ] 点击 "Notes" 面板展开
- [ ] 默认显示本地存储的 Notes（如有）
- [ ] 点击 "+ New Note" 弹出编辑框
- [ ] 编辑内容并保存，列表中新增 Note 条目
- [ ] 刷新页面，Notes 依然存在 (LocalStorage)
- [ ] 点击已有 Note，弹出编辑框且内容正确
- [ ] 修改 Note 并保存，内容更新

## 3. 拖拽与引用 (Drag & Drop)
- [ ] 从左侧拖拽 Run 条目到聊天输入框区域
- [ ] 输入框上方出现 Run 的引用 Chip (e.g., Run #123)
- [ ] 从左侧拖拽 Note 条目到聊天输入框区域
- [ ] 输入框上方出现 Note 的引用 Chip
- [ ] 点击 Chip 的 "x" 按钮，Chip 被移除
- [ ] 点击 "Clear All"，所有 Chips 被移除

## 4. 聊天交互 (Chat)
### 纯文本对话
- [ ] 输入 "Hello"，点击 Send
- [ ] 消息列表追加 User 消息
- [ ] 收到 AI 回复 (Mock 或 真实)
- [ ] 输入框清空

### 带引用对话
- [ ] 拖拽一个 Run，输入 "Analyze this run"
- [ ] 发送后，后端日志/网络请求确认 payload 包含 attachment
- [ ] AI 回复包含该 Run 的信息 (证明后端正确解析并传给 AI)

### 异常处理
- [ ] AI Disabled 状态 (后端配置 enable=false)：
  - [ ] 发送消息，收到 "AI 未启用" 错误提示
  - [ ] 界面显示红色错误 Banner
  - [ ] 页面其他功能 (Notes 编辑, 拖拽) 仍可用

## 5. 布局与响应式
- [ ] 拖拽左侧边栏边缘，调整宽度
- [ ] 窗口缩放时，布局自适应
