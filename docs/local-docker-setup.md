# 本地 Docker 启动说明（V2）

## 1. 前置准备
- 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)。
- 首次安装后确认 Docker Desktop 已启动，状态为 `Engine running`。
- 如果你已经有自己的 MySQL / MinIO，也可以不使用本文的 Compose，直接改环境变量连接外部实例。

## 2. 复制环境变量
在仓库根目录执行：

```powershell
Copy-Item .env.example .env
```

如需改端口、数据库名或 MinIO 账号，直接编辑根目录 `.env`。

## 3. 启动 MySQL 和 MinIO
在仓库根目录执行：

```powershell
docker compose up -d
```

启动成功后可用以下命令查看状态：

```powershell
docker compose ps
```

## 4. 默认访问地址
- MySQL: `localhost:3306`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- 默认桶名: `resumes`

默认账号来自 `.env`：
- MinIO 用户名: `minioadmin`
- MinIO 密码: `minioadmin`
- MySQL root 密码: `root123456`

## 5. 启动后端
先确保 Compose 服务正常，再进入后端目录执行：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

`local` profile 会默认连接 Docker 中的 MySQL + MinIO。

## 6. 启动前端
进入前端目录执行：

```powershell
cd 招聘流程协同平台后台
npm install
npm run dev
```

## 7. 常见问题
### MySQL 连接失败
- 先执行 `docker compose ps` 确认 `mysql` 为 `healthy`。
- 检查 `.env` 中的 `DB_PORT`、`DB_PASSWORD` 是否和 Compose 一致。
- 如果 3306 被占用，修改 `.env` 后重新 `docker compose up -d`。

### MinIO 连接失败或桶不存在
- 打开 `http://localhost:9001` 检查 MinIO 是否可登录。
- 确认 `.env` 中 `MINIO_ENDPOINT`、`MINIO_BUCKET` 正确。
- 如果桶创建失败，执行：

```powershell
docker compose up -d minio minio-init
```

### 后端仍然连 H2
- 确认启动命令里带了 `-Dspring-boot.run.profiles=local`。
- 确认没有额外环境变量覆盖 `SPRING_PROFILES_ACTIVE=test`。

### 清理本地数据
如果需要重置 MySQL / MinIO 数据：

```powershell
docker compose down -v
docker compose up -d
```
