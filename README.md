# CDZS Starter

首次使用请阅读：[底座使用与启动配置教程](docs/底座使用与启动配置教程.md)，包含环境准备、Windows / IDEA 启动、配置、新项目开发和 Jenkins 发布。

## 业务开发入口

- 后端：`backend/cdzs-module-business`，使用 Controller → Service 接口 → ServiceImpl → Mapper。
- 前端：`frontend/src/views/business` 与 `frontend/src/api/business`。
- [业务模块开发约定](docs/业务模块开发约定.md) · [CDZS 命名迁移说明](docs/CDZS命名迁移说明.md)

## 已补充的复用能力

- **数据库版本管理**：Flyway 自动执行增量 SQL，已有数据库显式接管，保留账号密码。参见 [数据库版本管理](docs/数据库版本管理.md)。
- **标准业务示例**：独立 `cdzs-module-example` 与“开发示例 → 示例任务”页面，包含 CRUD、分页、校验、权限、租户及个人数据隔离、Excel 导入导出。参见 [标准业务模块开发](docs/标准业务模块开发.md)。
- **部署与恢复**：升级前备份，备份失败阻止发布，隔离恢复演练和显式数据库恢复。参见 [部署备份与恢复](docs/部署备份与恢复.md)。

新库不再手工执行 `database/init/001-base.sql`，后端启动时由 Flyway 创建。旧版本升级请先阅读数据库接管步骤。

基于芋道官方精简版的独立项目基础版。复制后开发自己的业务，不依赖 Rapid 私有包或现有采购系统。

## 已包含

- 用户、部门、岗位、角色、菜单、按钮/接口权限、部门数据权限。
- 站内信、短信、邮件模板与发送日志（短信及邮件无演示渠道，接入自己的凭据后使用）。
- 登录日志、操作日志、接口访问与异常日志。
- 字典、参数、文件上传、代码生成。
- 独立数据库初始化、随机初始密码、前后端同源部署。
- Jenkins 参数化构建、镜像发布、健康检查、应用版本回滚。

源码版本和许可证见 [UPSTREAM.json](UPSTREAM.json)、backend/LICENSE、frontend/LICENSE。后端 Java 17 / Spring Boot 3.5.15，前端 Vue 3 / Element Plus；具体依赖以源码和锁文件为准。上游其他业务前端源码暂保留作为参考，但不在基础版路由或构建入口中启用。

## Windows 本地开发

准备 JDK 17、Maven 3.9、Node 20.19+（建议22）、pnpm 10.33、Docker Desktop Linux 容器。

```powershell
cd D:\code\cdzs-starter
.\scripts\dev.ps1 init
.\scripts\dev.ps1 infra
```

分别在两个终端启动：

```powershell
.\scripts\dev.ps1 backend
```

```powershell
.\scripts\dev.ps1 frontend
```

打开 http://localhost:8080 。租户：`默认租户`；账号：`admin`；密码：项目根目录 `.env` 中的 `ADMIN_INITIAL_PASSWORD`。密码随机生成，不是上游演示密码。登录后可以修改；重启不会重置。

开发模式与全容器模式均使用 `http://localhost:8080`。已初始化环境更换访问域名，请到“基础设施 → 文件配置”修改数据库存储的域名；该配置不会在每次重启覆盖。

数据库、Redis 分别只绑定本机 `13306`、`16379`，不会使用现有采购数据库。`stop` 只停止容器，不删除数据卷。

## 全容器启动

```powershell
.\scripts\dev.ps1 all
```

第一次需要下载依赖和构建镜像，完成后访问 http://localhost:8080 。本地开发模式与全容器模式择一运行，避免同时启动两个后端连接同一数据库。

Linux 对应命令为 `bash scripts/dev.sh init|infra|backend|frontend|all|stop`。

## 后续使用

- [从基础版创建新项目](docs/新项目开发.md)
- [Jenkins 首次配置与发布](docs/Jenkins部署.md)
- [基础功能配置与验收](docs/基础功能与验收.md)
- [验证记录](docs/验证记录.md)

新库由后端启动时通过 Flyway 执行 V1/V2。后续增量 SQL 放在 `backend/cdzs-server/src/main/resources/db/migration/`，发布前备份；不再手工导入 `database/init/001-base.sql`。旧版数据库按 [数据库版本管理](docs/数据库版本管理.md) 显式接管，保留既有账号。
