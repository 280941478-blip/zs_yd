# Jenkins 首次配置与发布

## 一、准备执行节点

创建 Jenkins **Pipeline** 任务，使用 SCM 拉取本项目根目录的 `Jenkinsfile`。不是旧式 Maven Project 任务。配置 Git 仓库 URL 和读取凭据。

Linux 执行节点标签设为 `docker`，安装 Git、Docker Engine（支持 BuildKit）、Docker Compose v2.24+、Bash、OpenSSH、tar。流水线在容器中编译，节点无需单独安装 Java、Maven、pnpm。

安装 Pipeline、Git、Credentials Binding、SSH Agent、Pipeline Utility Steps 插件。Docker 操作权限等同高权限，只给受信任的代码发布任务使用。

## 二、设置镜像与服务器

编辑 `deploy/targets.json`：

- registry：镜像路径，例如 `registry.example.com/team`。
- registryHost：登录域名，例如 `registry.example.com`。
- project：新项目名。
- test/prod：服务器地址、SSH 用户、端口、部署目录、凭据 ID。

不同环境使用不同部署目录和 `.env` 中的 `COMPOSE_PROJECT_NAME`。默认生产不自动发布，选择 DEPLOY 才发布。

## 三、添加 Jenkins 凭据

| ID 默认值 | 类型 | 用途 |
|---|---|---|
| starter-registry | 用户名与密码 | 镜像推送；密码推荐仓库访问令牌 |
| starter-test-ssh | SSH 私钥 | 测试机部署 |
| starter-prod-ssh | SSH 私钥 | 生产机部署 |
| starter-known-hosts | Secret file | 已核验服务器指纹的 known_hosts 文件 |

不关闭 SSH 主机指纹校验。Git 读取凭据在 Jenkins 任务 SCM 中设置。流水线不会修改现有采购 Jenkins 任务。

## 四、服务器首次准备

安装 Docker/Compose、Bash、flock（util-linux），给部署用户授权自己的部署目录和 Docker。服务器用独立只读凭据执行一次 `docker login`，用于拉取私有镜像。

例如部署目录 `/opt/yudao-starter-test`：将项目 `.env.example` 和 scripts/init-env.cjs 按原目录关系放到临时准备目录，运行 `node scripts/init-env.cjs` 生成随机密码，然后把 `.env` 放到该部署目录，权限设为600。也可手动填写所有 CHANGE_ME 项。

调整：
- COMPOSE_PROJECT_NAME：例如 yudao-starter-test。
- APP_PUBLIC_URL：实际外网访问地址（含 https 或 http）。
- HTTP_BIND：同机已有 Nginx/1Panel 反向代理时保留127.0.0.1；需要对外监听时改为服务器绑定地址。
- HTTP_PORT：与现有服务不冲突的端口。
- DB_NAME、DB_USER 及数据库、Redis、初始管理员密码。

不要把服务器 `.env` 放入 Git 或 Jenkins 构建产物。MySQL/Redis 默认不发布主机端口。TLS 由服务器已有的 Nginx/1Panel 入口管理，转发到 HTTP_PORT。

## 五、发布

1. 首次选 BUILD_ONLY、SCOPE=all，验证镜像构建与后端测试。
2. 配置凭据和服务器后，选 DEPLOY、TARGET_ENV=test、SCOPE=all。
3. GIT_REF 留空使用任务分支，也可填写分支/标签。
4. 自动构建 → 推送带构建号和提交号的镜像 → 复制部署文件 → 拉镜像 → 启动 → 健康检查。
5. 登录验证后再选择生产环境发布。

后续可以仅发布前端或后端；未更新的一端沿用当前记录的镜像。后端变更后会重建 Nginx 容器，刷新后端容器 DNS。

发布记录在服务器 releases/版本号 和 .current-release；不是零停机部署，服务替换期间可能短暂不可用。

## 六、回滚

选择 ACTION=ROLLBACK，填已有版本号；留空回到上一成功版本。部署健康检查失败时，有上一成功版本则尝试自动恢复前后端。

只回滚应用镜像和对应 Compose 文件，不回滚数据库。发布包含不兼容数据库变更时，必须先确认旧代码兼容性。失败日志在服务器 `docker compose logs`，健康检查本身不会将数据库自动恢复。

不要删除数据卷，不要直接在生产运行 `down -v`。后端升级会先备份 MySQL，备份失败则中止；新迁移由 Flyway 在后端启动时执行。恢复步骤见 [部署备份与恢复](部署备份与恢复.md)。
