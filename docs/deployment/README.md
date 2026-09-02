# 当前 SaaS 版本部署说明

更新日期：2026-09-02。适用于 `saas-platform`，默认启用 system、infra、pay、mp、homemaking。

本说明按“单台 Linux 主机运行 Java + Nginx，连接 MySQL/Redis”的方式提供模板；Windows 可负责打包和本地初始化。服务器地址、实际域名和账号尚未提供，下面的目录是部署约定示例，**不是已经替你部署好的地址**。当前副本、原仓库目录、未来 Git 克隆目录都可以作为打包目录，不需要把 `.codex` 隐藏目录上传到服务器。

当前版本已通过本地构建与选定回归；原生 MySQL 迁移、部分旧业务功能、真实渠道验收仍未完成，见[验收报告](../migration/final-verification.md)。下面可用于准备部署和隔离预发布环境，正式切换前应完成这些项目。

## 1. 先准备什么

- 构建机：JDK 17、Maven、Node、pnpm。当前本地验证版本为 Java 17.0.14、Node 24.14.0、pnpm 11.19.0；前端 packageManager 锁定 pnpm 11.19.0。前端检查脚本允许最高 8 GiB Node 堆，打包机需有相应可用内存。
- 服务器：Java 17、Nginx、可访问的 MySQL **8.0.16+** 和 Redis。MySQL 5.7 不满足迁移断言的 CHECK 约束要求。
- 实际配置：数据库主机/端口/库名/账号/密码，Redis 主机/端口/编号/密码，API 与网站 HTTPS 域名，证书及持久目录。
- 开通微信/支付时另外准备真实 AppID、AppSecret、开放平台归属、商户配置、通知模板。它们不会在打包时自动生成。

完整替换清单在[环境变量与替换位置](environment-variables.md)。先填清单，再执行下面步骤。

## 2. 复制两份配置模板

| 模板 | 复制到哪里 | 何时修改 |
| --- | --- | --- |
| [backend.env.example](../../deploy/backend.env.example) | 服务器私有文件，例如 `/etc/hm/backend.env` | 启动后端前，替换全部 `CHANGE_ME` |
| [frontend.prod.env.example](../../deploy/frontend.prod.env.example) | 打包目录 `hm-ui/.env.prod.local` | **前端打包前**；默认同域部署可保留空 `VITE_BASE_URL` |
| [hm-server.service.example](../../deploy/hm-server.service.example) | `/etc/systemd/system/hm-server.service` | 核对 Java、JAR、环境文件路径和运行账号 |
| [nginx.conf.example](../../deploy/nginx.conf.example) | 服务器 Nginx 站点配置 | 替换域名、证书、静态文件目录及后端端口 |

首次复制前端模板，在仓库根目录运行 PowerShell：

```powershell
if (!(Test-Path hm-ui/.env.prod.local)) {
  Copy-Item deploy/frontend.prod.env.example hm-ui/.env.prod.local
}
```

然后打开 `hm-ui/.env.prod.local` 检查或修改。该文件及 `deploy/backend.env` 已加入 Git 忽略；真实后端配置应存放在服务器私有位置。模板只有占位值，不含可用密码。

后端配置没有嵌入 JAR：同一个构建包可通过不同的环境变量连接预发布/生产库。不要为了替换数据库密码去编辑已经打包好的 JAR。

## 3. 打包：在仓库根目录执行

先确认当前是 `saas-platform`，记录准备发布的 commit：

```powershell
git branch --show-current
git rev-parse HEAD
java -version
node --version
pnpm --version
```

后端打包并运行本次选定的 34 项回归：

```powershell
mvn -s .mvn/settings.xml "-Dtest=BusinessIsolationTest,IdentityMappingTest,NotificationPolicyTest,PayOwnershipTest,DesensitizeTest" "-Dsurefire.failIfNoSpecifiedTests=false" package
```

这一步不连接生产数据库。结果应为 `BUILD SUCCESS`，产物为 `hm-server/target/hm-server.jar`。仅需重打包且该版本已完成测试时，可用 `mvn -s .mvn/settings.xml -DskipTests package`；不要把跳过测试的结果当成新的验收。

前端：

```powershell
Push-Location hm-ui
try {
  pnpm install --frozen-lockfile
  if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败' }
  pnpm build:prod
  if ($LASTEXITCODE -ne 0) { throw '前端打包失败' }
  pnpm ts:check
  if ($LASTEXITCODE -ne 0) { throw '前端类型检查失败' }
} finally {
  Pop-Location
}
```

先 build 再检查，是因为本项目的自动导入声明由 Vite 插件生成，干净安装时可能尚不存在。任何一步失败都应修复后再发布。

产物是 **`hm-ui/dist-prod`**，不是 `dist`，也不是旧项目的 `hm-ui-admin`。打包使用 `--mode prod`，不会自动使用你另建的 `.env.production`。前端改了域名/构建变量必须重新执行这一步。

## 4. 上传什么，放在哪里

示例发布目录为 `/opt/hm/releases/release-001`，`/opt/hm/current` 指向当前发布目录。发布编号按实际版本填写。

| 本地文件 | 示例服务器位置 |
| --- | --- |
| `hm-server/target/hm-server.jar` | `/opt/hm/releases/release-001/hm-server.jar` |
| `hm-ui/dist-prod/` **里面全部文件和目录** | `/opt/hm/releases/release-001/web/`；确保这里直接有 index.html |
| `sql/mysql/hm-base.sql`、`hm-pay-mp.sql`、`hm-bootstrap.sql`、`hm-homemaking.sql`、`hm-menu.sql` | 同次发布的 `sql/mysql/`，仅初始化/演练时使用 |
| `tools/bootstrap/SetAdminPassword.java` | 同次发布的 `tools/bootstrap/`，首次初始化管理员使用 |
| 填好的后端环境文件 | `/etc/hm/backend.env`，**不在 web 目录中** |
| 日志 | `/var/log/hm/`，与版本目录分离 |
| 本地上传文件（如采用本地存储） | `/var/lib/hm/uploads/`，在后台文件配置中填写此持久目录 |

服务器上先创建专用运行账号 `hm` 和上述目录；JAR/静态资源可读，日志和上传目录由 `hm` 可写。`backend.env` 只允许运维/服务管理器读取，例如 root 所有、权限 600。模板路径可改，但 Nginx、systemd 和文件存储配置必须同步。

不要上传 `node_modules`、`.git`、本机 `.runtime` 测试库或测试配置。前端服务器只需要 `dist-prod` 产物，不需要运行 pnpm 开发服务器。

## 5. 初始化数据库：只用于新空库

连接信息以你填写的 `HM_DB_URL` 为准。修改配置不等于创建数据库；数据库和账号要先由数据库管理员建立。下面 SQL 中每个 `CHANGE_ME` 都需替换：

```sql
CREATE DATABASE `CHANGE_ME_DATABASE` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'CHANGE_ME_DB_USER'@'CHANGE_ME_APP_SOURCE_HOST' IDENTIFIED BY 'CHANGE_ME_DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON `CHANGE_ME_DATABASE`.* TO 'CHANGE_ME_DB_USER'@'CHANGE_ME_APP_SOURCE_HOST';
```

`CHANGE_ME_APP_SOURCE_HOST` 是数据库看到的应用连接来源，不是网站域名。导入表结构使用单独的安装账号（需 DDL 权限）；上面的运行账号仅提供默认运行 DML 权限。代码生成/运维功能若需要更多权限，应单独评估。

在**同一个新目标库**依次执行：

1. `sql/mysql/hm-base.sql`
2. `sql/mysql/hm-pay-mp.sql`
3. `sql/mysql/hm-bootstrap.sql`
4. `sql/mysql/hm-homemaking.sql`
5. `sql/mysql/hm-menu.sql`

例如 Linux shell，在上传 SQL 的发布目录执行（命令中的主机/账号/库名全部替换）：

```bash
mysql --host=CHANGE_ME_DB_HOST --port=3306 --user=CHANGE_ME_INSTALL_USER --password --default-character-set=utf8mb4 --ssl-mode=REQUIRED CHANGE_ME_DATABASE < sql/mysql/hm-base.sql
mysql --host=CHANGE_ME_DB_HOST --port=3306 --user=CHANGE_ME_INSTALL_USER --password --default-character-set=utf8mb4 --ssl-mode=REQUIRED CHANGE_ME_DATABASE < sql/mysql/hm-pay-mp.sql
mysql --host=CHANGE_ME_DB_HOST --port=3306 --user=CHANGE_ME_INSTALL_USER --password --default-character-set=utf8mb4 --ssl-mode=REQUIRED CHANGE_ME_DATABASE < sql/mysql/hm-bootstrap.sql
mysql --host=CHANGE_ME_DB_HOST --port=3306 --user=CHANGE_ME_INSTALL_USER --password --default-character-set=utf8mb4 --ssl-mode=REQUIRED CHANGE_ME_DATABASE < sql/mysql/hm-homemaking.sql
mysql --host=CHANGE_ME_DB_HOST --port=3306 --user=CHANGE_ME_INSTALL_USER --password --default-character-set=utf8mb4 --ssl-mode=REQUIRED CHANGE_ME_DATABASE < sql/mysql/hm-menu.sql
```

每条执行成功后再执行下一条，密码交互输入。Windows PowerShell 不支持上述 `<` 写法，可使用数据库客户端选择目标库逐份执行。无需机械替换表名前缀，也不要用旧 `ruoyi-vue-pro.sql` 替代当前五份 SQL。

**已有数据库不能重跑初始化脚本。** 旧家政数据迁移另见[迁移操作说明](../migration/runbook.md#旧库迁移)：先备份到 `hm_legacy_snapshot`，在隔离目标库演练，再核对数据与回滚方案。当前没有自动增量数据库升级工具；以后更新版本要按该版本专门的升级 SQL 执行，不能重新初始化。

## 6. 首次管理员与后端启动

确认 `/etc/hm/backend.env` 的全部必填项已替换。Java 默认不会读取这个文件，下面二选一方式才能把它加载进进程。

**Linux 的首次管理员初始化（交互终端）**：已完成五份建库脚本，且 `/opt/hm/current` 指向上传好的发布目录后，用运维账号运行：

```bash
install -d -m 700 /opt/hm/bootstrap-tmp
cd /opt/hm/bootstrap-tmp
jar xf /opt/hm/current/hm-server.jar BOOT-INF/lib
set -a
. /etc/hm/backend.env
set +a
java -Dfile.encoding=UTF-8 -cp 'BOOT-INF/lib/*' /opt/hm/current/tools/bootstrap/SetAdminPassword.java
```

这里 `. /etc/hm/backend.env` 只加载你自己编写、受保护的文件。模板使用单引号，值不会展开 `$`；若密码本身含单引号，不要直接套用，需要正确处理环境文件转义或改用运维平台注入。

按提示输入两次 12–64 字符的密码，工具只修改首次锁定的总部 admin。已经初始化过的账号会被拒绝重设。Windows 本地/运维终端可以设置 `$env:HM_DB_URL`、`$env:HM_DB_USER`、`$env:HM_DB_PASSWORD` 后，在仓库根目录运行 `./tools/bootstrap/bootstrap-admin.ps1`；必须指向准备初始化的新库。

**正式进程使用 systemd**：核对并安装服务模板后运行：

```bash
systemctl daemon-reload
systemctl enable --now hm-server
systemctl status hm-server --no-pager
journalctl -u hm-server -n 100 --no-pager
```

服务模板读取 `/etc/hm/backend.env`；仅改了这个环境文件时，执行 `systemctl restart hm-server` 即可，不需重新打 JAR。改了服务文件本身，则先 daemon-reload 再重启。不要使用旧脚本的 `development` profile。模板未在你的真实 Linux 服务器运行验证，部署时以 status 和日志为准。

## 7. Nginx、网站与小程序

Nginx 模板需替换 `hm.example.invalid`、两处证书路径、`/opt/hm/current/web`；后端端口变动时也要替换 `48080`。它包含 HTTP 跳 HTTPS、未知 Host 拒绝、两个 API 前缀、WebSocket 和 SPA 路由回退。若服务器已有其他站点，先合并 `default_server` 配置，避免重复声明。

保留 `proxy_pass http://127.0.0.1:48080;` 不附加 URI 末尾斜杠，避免丢失 `/admin-api`、`/app-api` 前缀；WebSocket 需要对应 Upgrade/Connection 头。[Nginx 代理说明](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)、[WebSocket 说明](https://nginx.org/en/docs/http/websocket.html)可用于核对代理行为。

```bash
nginx -t
systemctl reload nginx
```

随后检查：

- HTTPS 网站可以打开；刷新 `/homemaking/settings` 等页面不会 404。
- `/app-api/homemaking/public/brand` 返回 JSON，不是 index.html。域名未在品牌页验证前 data 可能为空，不代表初始化失败。
- 用总部租户和初始化的 admin 密码登录，能看到家政、system/infra/pay/mp 对应导航。
- 在文件配置中创建并设置主存储，验证合法图片上传；空库没有可直接使用的存储账号。
- 品牌页登记域名并完成 `_hm-verification.域名` TXT 验证；DNS A/AAAA、Nginx 与证书也需匹配。

小程序修改 `hm-miniapp/app.js` 的 HTTPS baseUrl 和 tenantId、`project.config.json` 的 AppID，核对开发者工具当前 AppID及合法域名后重新上传。完整清单见[小程序替换位置](environment-variables.md#小程序这三处必须逐项确认)。

## 8. 接入真实渠道之前

这些配置在数据库/管理页面里，不仅仅是环境变量：

- 微信应用先由总部注册 tenantId/appId/kind/platformId/secretEnv，再在品牌页绑定 AppID；secretEnv 指向 Java 进程里真实存在的秘密变量。
- mp 模块的公众号账号、Token/EncodingAESKey、微信后台配置需要分别完成。
- 支付渠道需真实商户证书/密钥，应用 appKey 与租户绑定；`HM_PUBLIC_API_URL` 生成渠道 `/admin-api/pay/notify/*` 回调前缀。家政支付应用的 `orderNotifyUrl` 使用 `https://实际域名/app-api/homemaking/public/payment-callback`。
- **当前家政没有独立退款业务通知接口**：退款状态同步依赖定时对账及人工同步；上游支付应用配置的 `refundNotifyUrl` 又是必填。不能随意填一个不存在的 URL 或把支付回调当退款回调。正式启用退款前须补齐受控退款回执处理并完成真实渠道验收，这是代码缺口，不是再加一个环境变量就能解决。
- 通知需真实模板映射、用户订阅/偏好和短信渠道配置。先保持 `HM_HOMEMAKING_NOTIFICATION_DELIVERY_ENABLED=false`，验收后才开启。

## 9. 后续升级与常见定位

每次发布记录 Git commit、JAR、前端整包及数据库升级脚本。上传到新的版本目录，停止旧服务后切换 `/opt/hm/current`，再启动并检查；不要只替换单个前端 JS 文件。保留前一版本产物。涉及数据库变化时，回滚必须同时考虑数据兼容性，不能仅换回旧 JAR。

| 现象 | 优先检查 |
| --- | --- |
| 缺少 `HM_DB_PASSWORD` / `HM_DATA_ENCRYPTION_KEY` 等占位变量 | backend.env 是否被服务管理器加载；Java 的运行用户/服务是否读得到配置 |
| 连接了旧库或提示 Unknown database | `HM_DB_URL` 的主机、端口和 `/数据库名`；运行命令是否仍有覆盖 datasource 的旧参数 |
| Access denied / Redis NOAUTH | 账号、密码、来源主机授权；Redis 密码是否一致 |
| MySQL TLS 握手失败 | 数据库 TLS 能力、证书和 Java 信任设置；不要当成密码错误反复更换密钥 |
| 前端仍请求 localhost:48080 | 是否使用 `.env.prod.local`，是否残留终端 `VITE_BASE_URL`，是否重新打包并上传了整个 dist-prod |
| 刷新页面 404 / API 返回 HTML | Nginx SPA 回退和 API 两个独立 location 是否正确 |
| 登录验证码不一致 | 前端生产开关是否与 `HM_PROFILE=prod` 后端一致 |
| 微信应用尚未配置 | 数据库应用注册、tenantId、AppID 和品牌绑定；不是仅填写全局 SDK 变量 |
| 通知没有发送 | 开关、平台上限、租户规则、客户偏好、订阅额度、模板和通知记录状态 |
| 上传失败提示主配置缺失 | 后台文件配置是否已创建并设为主配置 |

旧的 `script/shell/deploy.sh`、`script/docker/docker-compose.yml` 及其环境文件保留为上游参考，尚未适配当前 SaaS 配置与目录。**此次按本说明和 `deploy/*.example` 操作，不直接运行旧脚本。**
