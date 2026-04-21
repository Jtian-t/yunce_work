# IDEA 连接 MySQL 与 MinIO 指南

本文说明如何在 IntelliJ IDEA 中查看本项目本地 Docker 启动的 MySQL 和 MinIO。

适用前提：

- 已执行 `docker compose up -d`
- MySQL 和 MinIO 容器已经正常启动
- 项目根目录 `.env` 已按实际端口和账号配置完成

参考默认配置：

- MySQL Host: `localhost`
- MySQL Port: `3306`
- MySQL Database: `recruit_platform`
- MySQL Username: `root`
- MySQL Password: `root123456`
- MinIO Endpoint: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- MinIO Access Key: `minioadmin`
- MinIO Secret Key: `minioadmin`
- MinIO Bucket: `resumes`

如果你改过 `.env`，请以你自己的配置为准。

## 1. 在 IDEA 中连接 MySQL

### 方式一：使用 Database 工具窗口

1. 打开 IDEA
2. 打开右侧或顶部的 `Database` 工具窗口
3. 点击 `+`
4. 选择 `Data Source` -> `MySQL`
5. 填入以下连接信息：

```text
Host: localhost
Port: 3306
User: root
Password: root123456
Database: recruit_platform
```

6. 如果 IDEA 提示下载驱动，点击 `Download missing driver files`
7. 点击 `Test Connection`
8. 成功后点击 `Apply` 或 `OK`

### 如果你使用的是非默认端口

比如你的 `.env` 中是：

```env
DB_PORT=3307
```

那么 IDEA 里端口也要改成 `3307`。

### 连接成功后你能看到什么

连接成功后，可以在 IDEA 中直接看到这些表：

- `candidate`
- `resume_asset`
- `department`
- `department_assignment`
- `department_feedback`
- `interview_plan`
- `interview_evaluation`
- `workflow_event`
- `agent_job`
- `agent_result`
- `notification`
- `report_snapshot`
- `platform_user`
- `refresh_token`
- `user_roles`

你也可以直接执行 SQL，例如：

```sql
show tables;
select * from candidate;
select * from platform_user;
```

## 2. 在 IDEA 中查看 MinIO

先说明一件事：

- MySQL 是关系型数据库
- MinIO 不是数据库，它是对象存储

所以 IDEA 不能像连接 MySQL 一样，把 MinIO 当作“数据库表”直接连。

### 推荐方式：通过 MinIO Console 查看

1. 在浏览器打开：

```text
http://localhost:9001
```

2. 使用以下账号登录：

```text
Username: minioadmin
Password: minioadmin
```

3. 进入桶：

```text
resumes
```

4. 你就可以查看上传的简历文件对象

这是当前最稳妥、最直观的方式。

### 如果你想在 IDEA 里辅助查看

可以使用两种思路：

#### 方式一：在 IDEA 的 HTTP Client 中测试接口

你可以在 IDEA 新建一个 `minio-check.http` 文件，手动记录项目相关接口，例如：

```http
GET http://localhost:8080/actuator/health
```

以及项目里的简历预览接口：

```http
GET http://localhost:8080/api/candidates/{id}/resume/preview
Authorization: Bearer your-token
```

这适合验证后端是否已经成功把文件上传到 MinIO 并能正常预览。

#### 方式二：使用支持 S3 的外部工具

如果你想像文件管理器一样浏览 MinIO，建议使用：

- MinIO Console
- S3 Browser
- Cyberduck
- 其他支持 S3 协议的客户端

连接参数如下：

```text
Endpoint: http://localhost:9000
Access Key: minioadmin
Secret Key: minioadmin
Bucket: resumes
Region: 可留空
```

## 3. 常见问题

### MySQL 在 IDEA 中连不上

优先检查：

1. Docker 是否真的启动了 MySQL

```powershell
docker compose ps
```

2. 端口是否和 `.env` 一致
3. 本机 `3306` 是否被其他 MySQL 占用
4. 是否已经创建了 `recruit_platform` 数据库

### IDEA 提示驱动缺失

这是正常情况，第一次连接 MySQL 时通常需要下载驱动，直接点下载即可。

### MinIO 登录不上

优先检查：

1. `docker compose ps` 中 `minio` 是否是 `healthy`
2. 访问地址是否是 `http://localhost:9001`
3. 账号密码是否与 `.env` 一致

### 后端已经启动，但 MinIO 里没文件

这通常不是连接问题，而是业务上还没有上传简历文件。

可以先在页面中上传一个候选人简历，然后再去 MinIO Console 里查看 `resumes` 桶。

## 4. 本项目推荐的查看方式

开发时建议这样分工：

- 表结构、数据排查：用 IDEA 连接 MySQL
- 文件对象、桶内容排查：用 MinIO Console
- 接口联调：用 IDEA HTTP Client 或前端页面

这样效率最高，也最接近真实开发流程。
