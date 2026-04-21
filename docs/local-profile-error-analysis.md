# 后端 `local` 环境启动错误分析

## 背景

执行命令：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

目标环境：

- 数据库：Docker MySQL 8.4
- 对象存储：Docker MinIO
- Spring Profile：`local`

## 本次定位到的错误

### 1. `report_snapshot.payload_json` 字段长度超过 MySQL 限制

错误现象：

```text
Column length too big for column 'payload_json' (max = 16383); use BLOB or TEXT instead
```

根因：

- `ReportSnapshot.payloadJson` 原来使用 `@Column(length = 20000)`
- MySQL 在 `utf8mb4` 字符集下，`VARCHAR(20000)` 超过单列可接受范围
- Hibernate 在自动建表/更新表结构时失败，导致部分表结构未能正常创建

修复方式：

- 将 `payloadJson` 改为 `@Lob`，让 MySQL 使用 `TEXT/LONGTEXT` 类型存储

修复文件：

- `backend/src/main/java/com/recruit/platform/report/ReportSnapshot.java`

### 2. `notification.read` 列名触发 MySQL 语法错误

错误现象：

```text
create table notification (...)
read bit not null,
...
You have an error in your SQL syntax
```

根因：

- `Notification` 实体中的布尔字段名是 `read`
- Hibernate 直接生成列名 `read`
- `read` 在 MySQL DDL 语句里容易引发关键字/语法冲突，导致 `notification` 表创建失败

修复方式：

- 将列名显式改为 `is_read`

修复文件：

- `backend/src/main/java/com/recruit/platform/notification/Notification.java`

### 3. 本机 `8080` 端口被残留 Java 进程占用

错误现象：

```text
Web server failed to start. Port 8080 was already in use.
```

根因：

- 之前的本地启动验证进程没有完全退出
- 新的 Spring Boot 进程启动时无法绑定 `8080`

处理方式：

- 检查 `8080` 端口占用进程
- 停止残留的 Java 进程后重新启动

说明：

- 这是本地运行环境冲突，不是业务代码逻辑错误

## 修复结果

修复后重新执行：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

验证结果：

- 应用以 `local` profile 成功启动
- 后端成功连接 MySQL 8.4
- `notification` 表已成功创建
- `/actuator/health` 可正常响应

## 建议

### 开发阶段建议

- 对大文本 JSON 字段优先使用 `@Lob`
- 避免直接使用可能冲突的列名，如 `read`、`order`、`key`
- 本地重复启动前先确认 `8080` 是否已被旧进程占用

### 后续建议

- 从 `ddl-auto=update` 逐步迁移到 Flyway/Liquibase
- 为真实 MySQL 场景补正式建表脚本和索引脚本
- 将本次问题整理进部署文档，减少后续环境排障成本
