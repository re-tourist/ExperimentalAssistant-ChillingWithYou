# 项目启动顺序（开发环境）

net start MySQL57
$env:DB_USERNAME="root"
$env:DB_PASSWORD="xyjsql"

cd backend
.\mvnw.cmd spring-boot:run


cd frontend
npm run dev

mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS experimental_db DEFAULT CHARACTER SET utf8mb4;"

npm install


mysql -u root -p experimental_db

## 0. 前置要求

- JDK 17
- Node.js `^20.19.0 || >=22.12.0`
- MySQL 8（本地安装或 Docker）

## 1. 数据库（MySQL）

### 方式 A：用 Docker 启动（推荐）

```powershell
# 启动 MySQL 8，并创建数据库 experimental_db（把 <...> 替换成你自己的值）
docker run --name experimentalassistant-mysql -e MYSQL_ROOT_PASSWORD=<DB_PASSWORD> -e MYSQL_DATABASE=experimental_db -p 3306:3306 -d mysql:8

# 查看容器日志，确认 MySQL 启动完成（可选）
docker logs -f experimentalassistant-mysql
```

### 方式 B：本地已安装 MySQL

```powershell
# 创建数据库 experimental_db（会提示输入 root 密码）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS experimental_db DEFAULT CHARACTER SET utf8mb4;"
```

### 用临时环境变量设置数据库账号/密码（当前终端会话生效）

后端读取环境变量 `DB_USERNAME` / `DB_PASSWORD`（也兼容 `MYSQL_USER` / `MYSQL_ROOT_PASSWORD`）。不设置则默认 `root` / 空密码。

```powershell
# 设置数据库账号（仅当前 PowerShell 窗口有效）
$env:DB_USERNAME="root"

# 设置数据库密码（仅当前 PowerShell 窗口有效）
$env:DB_PASSWORD="xyjsql"

# 清理环境变量（可选）
Remove-Item Env:DB_USERNAME, Env:DB_PASSWORD -ErrorAction SilentlyContinue
```

## 2. 后端（Spring Boot）

后端默认端口 `8080`（可通过环境变量 `SERVER_PORT` 覆盖）。数据库账号/密码建议用环境变量 `DB_USERNAME` / `DB_PASSWORD`（也兼容 `MYSQL_USER` / `MYSQL_ROOT_PASSWORD` / `MYSQL_PWD`）。表结构初始化由 `DB_INIT_MODE` 控制（需要时设为 `always`），脚本位于 `backend/src/main/resources/sql/`。

```powershell
# 进入后端目录
cd backend

# 可选：覆盖服务端口（默认 8080）
$env:SERVER_PORT="8080"

# 启动后端服务
.\mvnw.cmd spring-boot:run
```

## 3. 前端（Vue 3 + Vite）

前端开发服务器默认端口 `5173`，并把 `/api` 代理到后端（默认 `http://localhost:8080`）。如果后端端口不是 8080，可在前端启动前设置 `VITE_BACKEND_PORT` 或 `VITE_BACKEND_URL`。

```powershell
# 进入前端目录
cd frontend

# 可选：配置后端地址/端口（默认 http://localhost:8080）
$env:VITE_BACKEND_PORT="8080"
# 或
# $env:VITE_BACKEND_URL="http://localhost:8080"

# 安装依赖（首次或依赖变化后）
npm install

# 启动前端开发服务器
npm run dev
```
