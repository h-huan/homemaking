# HM 家政 SaaS

Java 17 模块化单体，Vue 3 + TypeScript + Element Plus。

默认启用 system、infra、pay、mp、homemaking。CRM、ERP、Mall、BPM、AI、MES、WMS、HRM、FMS、IM 等模块保留源码，默认不启用。

上游版本与许可见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。原应用存放于 legacy/，不参与新系统构建。

## 构建

部署前先阅读[当前版本部署步骤](docs/deployment/README.md)和[环境变量与替换位置](docs/deployment/environment-variables.md)。从 `deploy/*.example` 复制配置；前端在打包前填写 `.env.prod.local`，后端由服务器在启动时注入环境变量。

```sh
mvn -s .mvn/settings.xml -DskipTests package
cd hm-ui
pnpm install --frozen-lockfile
pnpm build:prod
pnpm ts:check
cd ../hm-portal
pnpm install --frozen-lockfile
pnpm build
```

## 配置与启动

新建空的 MySQL 8.0.16+ 数据库，依次执行 sql/mysql/hm-base.sql、hm-pay-mp.sql、hm-bootstrap.sql、hm-homemaking.sql、hm-menu.sql，再执行 sql/mysql/upgrades/V002__operations_and_portal.sql、V003__operations_menu.sql。已有 502a15e 版本数据库仅执行 V002、V003；不要重跑初始化 SQL。详见[增量升级说明](sql/mysql/upgrades/README.md)。
设置 HM_DB_URL、HM_DB_USER、HM_DB_PASSWORD、HM_DATA_ENCRYPTION_KEY（随机至少 16 字符）、HM_REDIS_HOST/PORT/PASSWORD。
生产新增 `HM_EVIDENCE_ROOT=/var/lib/hm/evidence`，用于私有履约照片；目录须可写、随数据库备份，不能映射到 Nginx 静态目录。
启动：`java -jar hm-server/target/hm-server.jar`。默认只监听 127.0.0.1:48080。
生产环境设置 HM_PROFILE=prod、HM_PUBLIC_API_URL=https://你的域名，配置可信反向代理与数据库 TLS。
微信公众号、小程序和支付凭据按租户配置，未配置时不发送真实消息或发起支付。
默认不提供通用初始账号密码。

总部默认直营，加盟租户沿用独立租户边界。客户身份属于平台，订单、地址、人员、服务和账目属于租户。

运营台：`/homemaking/operations`；白标与通知：`/homemaking/settings`；官网编辑：`/homemaking/portal`；排班：`/homemaking/scheduling`；人员工作台：`/homemaking/worker`；额度与结算：`/homemaking/finance`。小程序源码位于 `hm-miniapp`，发布前需设置真实 API 域名、租户编号和 AppID。

后台产物 `hm-ui/dist-prod`、官网产物 `hm-portal/dist` 分别部署到后台域名和已验证的租户官网域名。官网内容由后台保存草稿后发布；官网构建不需要秘密环境变量。所有新预约必须先配置人员技能、区域和实际班次。

- [初始化、账号配置和数据迁移](docs/migration/runbook.md)
- [打包、上传、数据库与服务器配置](docs/deployment/README.md)
- [部署变量逐项替换清单](docs/deployment/environment-variables.md)
- [凭据检查与历史风险](tools/security/README.md)
- [安全检查](docs/migration/security-review.md)
- [架构设计](docs/migration/design.md)

本分支提供可构建、可启动的迁移实现。生产数据尚未导入；真实微信、支付渠道和 MySQL 原生迁移演练仍需使用实际环境验收。

日常开业配置、规格/加项/区域、排班与人员角色、官网发布和财务操作见[运营手册](docs/deployment/operations.md)。

## 仓库维护约定

仅提交产品源码、必要测试、可复用工具、数据库升级、配置模板，以及部署/运维/架构/安全/来源许可等长期维护文档。同一主题优先更新已有说明。AI 阶段计划、进度记录、验收流水、聊天摘要、截图、调试脚本和本机环境文件存放于被忽略的 `.runtime/`，不提交到 Git；需要回顾的测试结果写在提交说明或 PR 中。新增运行参数或数据库升级时，必须同步现有部署说明和变量清单。
