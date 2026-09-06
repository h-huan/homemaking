# HM 初始化与迁移操作说明

完整打包、上传、环境变量替换和 Linux 启动流程见[当前部署说明](../deployment/README.md)；所有变量及填写位置见[替换清单](../deployment/environment-variables.md)。本页主要说明空库和旧数据迁移。

## 环境与空库初始化

使用 Java 17、MySQL **8.0.16 或更高版本**、Redis、Node 和项目锁定的 pnpm。迁移断言依赖 CHECK 约束，不支持将 MySQL 5.7 的执行结果当成验收。新库使用 utf8mb4，应用时区与数据库时区统一为 Asia/Shanghai。

新建空数据库后，**只导入一次 `sql/mysql/hm-init.sql`**。文件已包含基础平台、支付公众号、普通直营租户与锁定平台管理员、家政领域、菜单，以及当前全部业务结构；无需额外执行分段 SQL 或这些版本脚本。

已有 SaaS 数据库先备份并验证可恢复性，只执行尚未应用的增量版本，详见[增量升级说明](../../sql/mysql/upgrades/README.md)。不要向旧库覆盖导入 hm-init.sql。默认总部租户编号 1、经营方式 DIRECT、支付方式 OFFLINE。FRANCHISE 为加盟模式；新增租户需在套餐里配置家政菜单权限，并分别建立其门店、服务、人员能力/排班和品牌配置。

## 私有配置与首次登录

在部署环境配置 `HM_DB_URL`、`HM_DB_USER`、`HM_DB_PASSWORD`、`HM_REDIS_HOST`、`HM_REDIS_PORT`、`HM_REDIS_PASSWORD`、`HM_DATA_ENCRYPTION_KEY`。加密密钥使用随机值（至少 16 字符）并持久备份，不要随重启变更。生产设置 `HM_PROFILE=prod`、`HM_PUBLIC_API_URL=https://实际服务域名`。通过私有环境文件或密钥管理设施注入，不提交到 Git。

首次管理员 admin 在数据库中是不可登录的锁定占位。构建后，在交互终端、正确设置数据库环境变量的情况下执行：

```powershell
./tools/bootstrap/bootstrap-admin.ps1
```

工具读取两次隐藏输入，生成 BCrypt，且只能修改总部首次锁定账号。不会打印或保存明文密码，不会重设已初始化账号。Linux 可在临时目录从服务器 JAR 提取 `BOOT-INF/lib`，再以这些 JAR 为 classpath 运行 `tools/bootstrap/SetAdminPassword.java`。

后端：`java -jar hm-server/target/hm-server.jar`。默认仅监听 127.0.0.1:48080。后台产物 `hm-ui/dist-prod` 与官网产物 `hm-portal/dist` 分别部署到后台、官网域名。后台代理 `/admin-api` 和 `/app-api`，官网代理 `/app-api`；后台 SPA 路由包括 `/wechat/callback` 必须回退到后台 index.html。可信代理需正确转发已验证 Host 并拒绝未知域名，外部必须使用 HTTPS。

设置 `HM_EVIDENCE_ROOT=/var/lib/hm/evidence` 保存私有履约照片和评价图片，目录在发布目录与所有网站静态目录之外。数据库与此目录一起备份；更新 JAR 或静态包时不能删除图片。生产模板与目录权限见部署说明。

小程序 `hm-miniapp/app.js` 中设置各租户的正式 HTTPS baseUrl 和 tenantId，使用对应 AppID 构建发布；旧登录 token 不兼容，升级后重新微信登录。微信后台配置 request 合法域名。

## 白标、微信和支付

品牌页面可设置官网、Logo、favicon、颜色、登录页标题/背景、首页模块、经营方式和应用标识。小程序首页消费 services/stores/workers/reviews/contact/banners 开关；轮播素材读取迁移后的首页内容。域名先申请验证 token，再在 `_hm-verification.域名` 配置 TXT 记录，验证后才用于识别租户。

平台管理员使用 `POST /admin-api/homemaking/wechat-apps` 注册应用：tenantId、appId、kind（MINI/MP）、platformId（微信开放平台真实归属）、secretEnv、enabled。secretEnv 只能使用 `HM_WECHAT_MINI..._SECRET` / `HM_WECHAT_MP..._SECRET` 形式，值在部署环境保存。然后在租户品牌中绑定 AppID。相同开放平台下的已验证 UnionID 才能跨应用关联同一 Customer；客户端提交的 OpenID 不能建立身份。公众号 OAuth 回调域名必须为已验证的租户域名。

公众号菜单/素材等使用 mp 模块账户；家政身份注册和通知模板也需配置对应应用。支付模块创建该租户的支付应用与 wx_lite 渠道，渠道 AppID 必须与小程序一致，品牌中填写支付应用 appKey。支付应用的业务订单通知地址指向 `https://实际域名/app-api/homemaking/public/payment-callback`；渠道通知使用环境配置的 `/admin-api/pay/notify/*` 地址。使用真实商户回调验签后，再验收支付/退款到账及金额核对。

## 通知中心

pay 应用的 `refundNotifyUrl` 填写 `https://实际API域名/app-api/homemaking/public/refund-callback`。该接口接收 pay 模块验签并落库后的业务通知，不能用作微信原始回调。家政根据 payRefundId 查询实际租户和退款记录，校验订单、商户退款号及金额后幂等更新，退款对已分佣订单产生反向明细。上线前完成[真实渠道验收](../deployment/README.md#8-接入真实渠道之前)。

默认不发送真实通知；配置完成并验收后设置 `HM_HOMEMAKING_NOTIFICATION_DELIVERY_ENABLED=true`。通知统一经过平台全局/事件上限、租户全局/事件规则、客户事件偏好。默认每日 5 条、间隔 30 分钟、22:00–08:00 静默；营销默认关闭。紧急消息可越过静默时段，仍受频控及客户选择限制。

租户页面设置通知规则；平台管理员可通过 `PUT /admin-api/homemaking/notification-policy?event=*&platform=true` 管理平台上限。`PUT /admin-api/homemaking/notification-template` 配置 event、channel（MP/MINI/SMS）、templateId、fieldMapping、enabled。模板字段必须对应微信审核模板或已有短信模板，不能通用复制某个账号的模板 ID。小程序订阅需用户授权并登记 consent；短信兜底仅对重要/紧急消息、且平台/租户/客户三方均允许时生效。

明确拒绝后才尝试下一允许渠道；超时或崩溃后结果不确定标为 UNKNOWN，需运营人员先查服务商记录，不能自动重发。SMS ACCEPTED 仅表示请求接受，不保证终端送达。通知记录页面提供状态和错误摘要。

## 旧库迁移

1. 将旧库制作成受保护的只读快照，数据库名为 `hm_legacy_snapshot`；验证来源表与 `legacy/sql/homemaking_p0.sql` 一致。不要在运行中的旧库直接改表。
2. 在隔离 MySQL 新空库导入 hm-init.sql 建表。检查组织归属、重复分类名称、孤立客户/服务、预约时间格式、退款和重复评价。脚本默认旧组织转总部租户 1 下的门店，不自动把旧组织当成加盟商。
3. 从新库运行 `sql/mysql/hm-migrate-legacy.sql`，批处理遇错立即终止，**不要使用 mysql --force**。先执行空库与数据约束断言，再在事务中导入；冲突需回滚并清理该次隔离目的库后重新演练。
4. 校验客户与租户关系、分类/门店/服务/SKU/加项/地址数量，订单和退款总额（元转整数分），预约及人员占位；抽查已付款、已取消、已完成、退款订单。归档表用于追溯，包含业务个人信息，应仅限数据库管理员访问。
5. 原 AppID 与开放平台归属确认后，按 `hm-migrate-legacy-wechat.sql` 的变量说明迁入微信映射。未确认 AppID 时不要猜测。旧 session secret 不导入新认证体系。
6. 历史支付未与新 pay 模块自动关联，禁止直接自动退款。历史异常订单进入 MIGRATION_REVIEW；先人工核对原支付流水。旧人员配置和线索保存在 `hm_legacy_archive`；新技能/区域/排班模型与管理页已提供，但不会自动猜测旧归属关系。正式切换前按归档核对并在排班页录入，确认产能与现有预约一致。
7. 原生演练通过、差额为零且回滚恢复验证完成后，安排停写窗口、最终备份和正式导入。DNS/小程序正式流量切换属于后续部署操作，本次未执行。

## 验证命令

```sh
mvn -s .mvn/settings.xml -Dtest=BusinessIsolationTest,EvidenceStorageTest,IdentityMappingTest,NotificationPolicyTest,PayOwnershipTest,DesensitizeTest -Dsurefire.failIfNoSpecifiedTests=false package
cd hm-ui
pnpm install --frozen-lockfile
pnpm build:prod
pnpm ts:check
cd ../hm-portal
pnpm install --frozen-lockfile
pnpm build
```

完整 upstream 单元/集成套件需要其独立配置与依赖，不应将上面的选定回归当成全仓所有测试。可复现 H2 迁移映射检查见 `tools/migration/README.md`。
