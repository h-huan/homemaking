# 部署变量与替换位置

适用于当前 `saas-platform` 的默认五个模块，核对日期 2026-09-03。变量名直接核对 `hm-server/src/main/resources/application.yaml`、`application-prod.yaml`、家政模块、`hm-ui` 和 `hm-portal` 配置；不是旧 RuoYi 的配置名。

## 先分清什么时候替换

| 内容 | 在哪里填 | 什么时候生效 |
| --- | --- | --- |
| 数据库、Redis、后端域名、服务器密钥 | 复制 `deploy/backend.env.example` 到服务器私有文件，例如 `/etc/hm/backend.env` | Java 进程读取环境变量；修改后重启后端，通常不需要重打 JAR |
| 浏览器 API 地址、页面标题、前端功能开关 | 打包机器上的 `hm-ui/.env.prod.local`，从 `deploy/frontend.prod.env.example` 复制 | **打包前填写**，修改后重新 `pnpm build:prod` 并替换整套 `dist-prod` |
| 小程序 API 域名、租户编号、AppID | `hm-miniapp/app.js`、`hm-miniapp/project.config.json`，并核对开发者工具本地项目设置 | 修改后重新编译、上传、审核和发布小程序 |
| 微信应用归属、支付渠道、通知模板、文件存储 | 初始化完成后在管理后台或家政管理 API 设置，保存于业务库 | 按配置缓存刷新；它们**不会**因为填写 `.env` 自动建好 |
| Nginx 域名、证书、静态目录、后端端口 | 服务器的 Nginx 配置 | 检查配置后 reload |

后端不会自动加载任意 `.env` 文件，必须由服务管理器或 shell 将值放进 Java 的环境变量。微信租户密钥代码使用 `System.getenv`，单独写 Java `-D` 参数或自建 YAML 字段不能代替它。

## 后端：生产必须确认的项目

默认值表示代码行为，不表示可直接拿去生产使用。模板中的 `CHANGE_ME` 都要替换；端口、路径和数据库编号即使已有默认，也应按实际环境确认。

| 变量 | 代码默认值 | 应填写什么 |
| --- | --- | --- |
| `HM_PROFILE` | `local` | 生产必须为 `prod`；同时移除旧启动命令里的 `--spring.profiles.active=local/development` 和冲突的 `SPRING_PROFILES_ACTIVE` |
| `HM_DB_URL` | 本机 3306、数据库 `hm`，带 SSL/时区参数 | `jdbc:mysql://数据库主机:端口/数据库名?...`；**主机、端口、库名三个位置都检查**。库名须与导入 SQL 的目的库一致 |
| `HM_DB_USER` | `hm` | 已创建且有目标库权限的应用账号，不必是安装 SQL 使用的管理员 |
| `HM_DB_PASSWORD` | 无，必须提供 | 上述账号的实际密码 |
| `HM_DATA_ENCRYPTION_KEY` | 无，必须提供 | 随机且持久的加密密钥；建议用 `openssl rand -hex 16` 生成 32 位十六进制字符串。部署后保持不变，和数据库一起备份；改密钥会影响既有密文解密 |
| `HM_REDIS_HOST` | `127.0.0.1` | Java 能访问的 Redis 主机；Docker 中的 127.0.0.1 是容器自身，不能照抄单机模板 |
| `HM_REDIS_PORT` | `6379` | 实际 Redis 端口 |
| `HM_REDIS_DATABASE` | `0` | 为本环境分配的数据库编号；不要混用开发、测试、生产缓存 |
| `HM_REDIS_PASSWORD` | 无，必须提供 | Redis 实际密码，需与 Redis 服务配置一致 |
| `HM_PUBLIC_API_URL` | `http://localhost:48080` | 对外可访问的 HTTPS 根地址，例如 `https://hm.example.com`；**不带 `/admin-api`、`/app-api` 或末尾 `/`**，用于渠道回调 URL |
| `HM_ADMIN_URL` | `http://localhost:5173` | 管理后台 HTTPS 根地址；默认同域部署时与上一项相同 |
| `HM_BIND_ADDRESS` | `127.0.0.1` | 本文 Nginx 与 Java 同机时保留；只有隔离网络/容器部署才按网络结构调整 |
| `HM_PORT` | `48080` | Java 监听端口；若修改，同时改 Nginx `proxy_pass` 和监控目标 |
| `HM_LOG_FILE` | `./logs/hm-server.log` | 持久日志文件，模板为 `/var/log/hm/hm-server.log`；运行账号必须可写 |

`HM_DB_URL` 模板使用 `sslMode=REQUIRED`，数据库必须支持 TLS。跨主机连接需要校验服务器身份时，使用 `sslMode=VERIFY_IDENTITY` 并配置可信证书。不要为了消除连接错误直接改成关闭 TLS。数据库 CA、账号授权来源主机、网络访问权限仍由实际数据库环境决定。

如果数据库名不是 `hm`，业务连接由 `HM_DB_URL` 决定。基础设施“代码生成器”另外读取 schema 名：需要该功能时设置 `SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_NAME` 为实际库名；这是 Spring 属性覆盖，不是另一个数据库连接地址。

## 后端：启用对应功能时才填写

| 变量 | 默认 | 使用范围与注意事项 |
| --- | --- | --- |
| `HM_HOMEMAKING_NOTIFICATION_DELIVERY_ENABLED` | `false` | 真实消息发送开关。完成模板、身份、用户偏好与渠道验收后才能设为 `true` |
| `HM_HOMEMAKING_NOTIFICATION_POLL_MS` | `15000` | 通知轮询周期，毫秒；默认可保留 |
| `HM_WECHAT_MP_APP_ID` / `HM_WECHAT_MP_SECRET` | 空 | 框架的公众号全局 SDK 配置；**不会自动创建** mp 账户或租户微信映射 |
| `HM_WECHAT_MINI_APP_ID` / `HM_WECHAT_MINI_SECRET` | 空 | 框架的小程序全局 SDK 配置；同样不能替代租户应用注册 |
| `HM_WECHAT_MINI_HQ_SECRET` | 没有自动默认 | 租户微信密钥的示例名称；只有注册应用的 `secretEnv` 指向它时才读取 |
| `HM_WECHAT_MP_HQ_SECRET` | 没有自动默认 | 公众号租户密钥示例；规则同上 |
| `HM_TENCENT_LBS_KEY` | 空 | 使用腾讯位置服务时的服务端 key |
| `HM_SUPPORT_EMAIL` | 空 | 平台说明中的联系邮箱 |
| `HM_API_REQUEST_KEY` / `HM_API_RESPONSE_KEY` | 空 | 可选 API 加密密钥；默认 API 加密关闭，填写密钥本身不会开启。启用时还要协调后端 `hm.api-encrypt.enable` 与前端协议，本文默认流程不启用 |

多租户的 `secretEnv` 只接受 `HM_WECHAT_MINI..._SECRET` 或 `HM_WECHAT_MP..._SECRET` 形式，例如 `HM_WECHAT_MINI_TENANT2_SECRET`。每个应用使用自己的真实密钥，环境变量名必须与数据库注册值完全一致。微信密钥不写进小程序源码、浏览器变量或 Git。

## 本轮新增：私有履约照片目录

`HM_EVIDENCE_ROOT` 在后端启动时读取，生产模板为 `/var/lib/hm/evidence`，开发默认 `./data/homemaking-evidence`。生产必须使用发布目录之外的绝对路径，提前创建并赋予后端账号读写权限；Nginx 不得将它设置为 root/alias。数据库只保存照片元数据，备份和恢复时必须同时处理数据库与此目录。单张照片仅支持 JPEG/PNG，最大 5 MB，最多 20 张/订单；服务端校验尺寸并重新编码去除 EXIF，查看接口核对客户/人员/管理员权限。

目录变动后重启后端；已有照片必须按原租户相对目录一起迁入新目录，不能仅修改变量。本轮无新增第三方密钥。

## 前端：打包前修改

当前 `package.json` 的 `build:prod` 实际执行 `vite build --mode prod`，应使用 `.env.prod.local`，不是 `.env.production`。[Vite 官方说明](https://vite.dev/guide/env-and-mode)说明了模式文件优先级和构建时替换行为；已经存在于打包进程中的环境变量优先级更高，构建前也应检查终端是否残留旧值。

| 变量 | 本次模板值 | 是否需要替换 |
| --- | --- | --- |
| `NODE_ENV` / `VITE_DEV` | `production` / `false` | 保留生产值 |
| `VITE_APP_TITLE` | `HM 家政管理平台` | 可改全局回退标题；租户白标加载后会覆盖对应标题 |
| `VITE_BASE_URL` | 空字符串 | Nginx 同域部署保留空；若采用独立 API 域名才填写 HTTPS 根地址，同时需要额外配置 CORS 和租户域名识别，不能只改这一项 |
| `VITE_API_URL` | `/admin-api` | 默认不改；不要把完整域名或重复的 `/admin-api` 填进去 |
| `VITE_BASE_PATH` | `/` | 当前示例按网站根目录部署；子目录部署还需同步改前端路由及公众号回调，不能只改此变量 |
| `VITE_OUT_DIR` | `dist-prod` | 保留，打包后上传 `hm-ui/dist-prod` 的**内容** |
| `VITE_UPLOAD_TYPE` | `server` | 保留；本次关闭了客户端直传入口 |
| `VITE_APP_TENANT_ENABLE` | `true` | 保留，与后端租户隔离一致 |
| `VITE_APP_CAPTCHA_ENABLE` | `true` | 与生产后端验证码开关保持一致 |
| `VITE_APP_DEFAULT_LOGIN_TENANT` | `HM 总部` | 改了总部租户名称才同步修改；品牌名与租户登录名称不是同一字段 |
| `VITE_APP_DEFAULT_LOGIN_USERNAME` / `VITE_APP_DEFAULT_LOGIN_PASSWORD` | 空 | 保留空，不写入管理员账号密码 |
| `VITE_BAIDU_MAP_KEY` | 空 | 使用地图页面时填写浏览器端 key，并在地图平台限制适用域名 |
| `VITE_APP_BAIDU_CODE` | 空 | 统计脚本默认不开启 |
| `VITE_APP_DOCALERT_ENABLE` | `false` | 默认不显示上游文档提示 |
| `VITE_APP_API_ENCRYPT_ENABLE` | `false` | 默认不开启；HEADER/ALGORITHM 保留模板值，REQUEST_KEY/RESPONSE_KEY 留空 |
| `VITE_DROP_DEBUGGER` / `VITE_DROP_CONSOLE` | `true` | 生产构建设置 |
| `VITE_SOURCEMAP` / `VITE_COMPRESS` | `false` / `none` | 默认保留 |
| `VITE_MALL_H5_DOMAIN` / `VITE_GOVIEW_URL` | 空 | 对应模块未启用，不填 |

`VITE_PORT`、`VITE_OPEN` 只影响开发服务器，不决定正式网站端口。前端模板明确覆盖生产相关项，避免仓库旧 `.env.local` 中的开发地址混入生产构建。所有 `VITE_*` 都可能进入浏览器产物，不能包含数据库密码、微信 AppSecret、商户私钥等服务端秘密。

## 小程序：这三处必须逐项确认

| 位置 | 当前仓库状态 | 发布前处理 |
| --- | --- | --- |
| `hm-miniapp/app.js` → `globalData.baseUrl` | `http://127.0.0.1:48080/app-api` | 改成 `https://你的域名/app-api`，必须保留一次 `/app-api` |
| 同文件 → `globalData.tenantId` | `1` | 总部为 1；加盟租户填实际编号，和该 AppID 的注册租户一致 |
| `hm-miniapp/project.config.json` → `appid` | 保留了旧项目 AppID | 替换为本次租户的真实 AppID，并核对开发者工具里的生效 AppID；不要默认沿用仓库旧值 |

同时在微信后台配置 HTTPS request 合法域名。公众号还要配置业务域名/OAuth 回调域名；应用归属、订阅消息模板和微信开放平台绑定要使用真实账号，不能从示例复制。

履约照片通过受权限保护的接口下载，小程序还需把同一个 HTTPS API 主机配置为 downloadFile 合法域名。不要把客户 token 放到图片 URL 查询参数中。

## 官网：无需填写秘密环境变量

`hm-portal` 使用同源 `/app-api/homemaking/public/portal`，由 Nginx 保留真实 Host 并转发到后端。构建产物为 `hm-portal/dist`，与 `hm-ui/dist-prod` 分别部署；两个项目按各自 packageManager 锁定的 pnpm 版本安装。官网没有 `VITE_*` 必填项，不复制后台的 `.env.prod.local`。

官网域名、Logo/favicon/主色在品牌页维护并验证；导航、图片、SEO、模块顺序和联系方式在“官网内容”页保存并发布。修改这些数据无需重打包；修改官网源码才重建静态包。若需要线上预约，主按钮填写真实预约页面 HTTPS 地址，或在联系模块填写门店电话/小程序码；不要将未配置的示例地址当成正式入口。

## 不在环境变量里的配置，也必须填写

| 功能 | 维护位置 | 要准备的值 |
| --- | --- | --- |
| 文件存储 | 总部 → 基础设施 → 文件配置 | 本地存储需持久 `basePath` 与访问 `domain`；S3 需 endpoint、bucket、accessKey、accessSecret、region/访问方式。空库没有默认存储，必须设置主配置才能上传 |
| 租户品牌/域名 | 家政运营 → 品牌与通知 | 网站、Logo/favicon URL、主色、登录页、AppID；域名要通过 TXT 验证，DNS A/AAAA 和 HTTPS 证书另行配置 |
| 微信统一身份 | `POST /admin-api/homemaking/wechat-apps`，仅总部可分配 | tenantId、appId、kind、真实开放平台归属 platformId、secretEnv、enabled；注册完成再绑定品牌 AppID |
| 公众号管理 | 公众号管理 → 账号 | AppID、AppSecret、Token、EncodingAESKey 等实际账号设置，与租户身份注册对应 |
| 支付应用与渠道 | 支付管理 → 应用/渠道；品牌页绑定 appKey | appKey、商户号、渠道 AppID、API 密钥/证书及回调配置；支付渠道 AppID 必须匹配小程序 |
| 短信 | 总部系统管理中的短信渠道、模板 | 服务商账号、签名、模板；与家政通知模板映射一致 |
| 通知模板 | `PUT /admin-api/homemaking/notification-template` | event、channel、真实 templateId、fieldMapping、enabled；短信兜底还受平台/租户/客户三级允许条件限制 |
| 排班与产能 | 家政运营 → 排班 | 人员所属门店、可服务项目、行政区、日期班次和请假；全区域需明确选择 *，未配置人员不会被自动派单 |
| 人员工作台 | 排班页绑定账号；系统角色授予 homemaking:worker | 同租户有效后台用户；客户微信账号不能作为人员后台账号使用 |
| 官网内容 | 家政运营 → 官网内容 | 导航、主视觉、SEO、首页模块/顺序、推荐内容与联系信息；先保存，再发布 |
| 配额与分佣 | 家政运营 → SaaS 与财务，仅总部修改 | 资源数、月订单/月短信上限、功能、分佣比例；-1 表示不限。保存套餐分配后在服务端生效 |
| 结算单 | SaaS 与财务 → 结算单 | 租户、门店/人员、账期；审核后登记真实打款凭证，再对账，不会自动打款 |

文件存储路径和日志目录必须在发布目录之外持久保存，不能随替换 JAR/网页包一起删除。付款、退款和真实消息的当前上线缺口见[部署步骤](README.md#8-接入真实渠道之前)。

规格/加项/区域费用与取消/改期规则在「家政运营 → 服务 → 价格与预约规则」维护，不需要新增环境变量。具体首次配置顺序见[运营手册](operations.md)。
