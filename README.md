# HM 家政 SaaS

Java 17 模块化单体，Vue 3 + TypeScript + Element Plus。

默认启用 system、infra、pay、mp、homemaking。CRM、ERP、Mall、BPM、AI、MES、WMS、HRM、FMS、IM 等模块保留源码，默认不启用。

上游版本与许可见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。原应用存放于 legacy/，不参与新系统构建。

## 构建

```sh
mvn -s .mvn/settings.xml -DskipTests package
cd hm-ui
pnpm install --frozen-lockfile
pnpm ts:check
pnpm build:prod
```

## 配置与启动

新建空的 MySQL 8.0.16+ 数据库，依次执行 sql/mysql/hm-base.sql、hm-pay-mp.sql、hm-bootstrap.sql、hm-homemaking.sql、hm-menu.sql。不要对已有生产库执行初始化 SQL。
设置 HM_DB_URL、HM_DB_USER、HM_DB_PASSWORD、HM_DATA_ENCRYPTION_KEY（随机至少 16 字符）、HM_REDIS_HOST/PORT/PASSWORD。
启动：`java -jar hm-server/target/hm-server.jar`。默认只监听 127.0.0.1:48080。
生产环境设置 HM_PROFILE=prod、HM_PUBLIC_API_URL=https://你的域名，配置可信反向代理与数据库 TLS。
微信公众号、小程序和支付凭据按租户配置，未配置时不发送真实消息或发起支付。
默认不提供通用初始账号密码。

总部默认直营，加盟租户沿用独立租户边界。客户身份属于平台，订单、地址、人员、服务和账目属于租户。

运营台：`/homemaking/operations`；白标与通知：`/homemaking/settings`。小程序源码位于 `hm-miniapp`，发布前需设置真实 API 域名和租户编号。

- [初始化、账号配置和数据迁移](docs/migration/runbook.md)
- [验收报告与当前边界](docs/migration/final-verification.md)
- [安全检查](docs/migration/security-review.md)
- [架构设计](docs/migration/design.md)

本分支提供可构建、可启动的迁移实现。生产数据尚未导入；真实微信、支付渠道和 MySQL 原生迁移演练仍需使用实际环境验收。
