# API_SPEC — 接口设计说明

项目名称：ExperimentalAssistant  
接口风格：RESTful  
返回格式统一为：
{
  "code": 0,
  "message": "ok",
  "data": ...
}

---

## 一、Projects

### GET /api/projects
分页获取项目列表

### POST /api/projects
创建项目

### PUT /api/projects/{id}
更新项目

### DELETE /api/projects/{id}
删除项目

---

## 二、Runs

### GET /api/runs
分页获取实验记录列表

参数：
- projectId
- status
- q
- dateFrom
- dateTo
- page
- size

返回说明：
- 包含基础实验信息
- 可包含 metricPreview（acc / loss / f1），用于列表展示

---

### GET /api/runs/{id}
获取实验详情

返回内容：
- 实验基础信息
- 标签列表
- 全部实验指标（metric_def + value）

---

### POST /api/runs
创建实验记录

请求体包含：
- 实验基础信息
- tagIds
- metrics（metricDefId + value）

---

### PUT /api/runs/{id}
更新实验记录（整体更新）

---

### DELETE /api/runs/{id}
删除实验记录

---

## 三、Dashboard

### GET /api/dashboard/summary
返回：
- totalRuns
- runsLast7Days
- successRate
- bestMetric

---

### GET /api/dashboard/trend
说明：
- 按天返回实验指标的每日最佳值（Best-of-day）
- 若 metric.direction = MAX，取每日 MAX(value)
- 若 metric.direction = MIN，取每日 MIN(value)

---

### GET /api/dashboard/distribution
参数：
- by = model | dataset | tag

---

### GET /api/dashboard/top
返回：
- 按指标方向排序的 TopN 实验记录

---

## 四、说明

- Dashboard 接口仅用于聚合查询
- CRUD 与统计查询逻辑分离
- 本系统不包含权限与认证模块