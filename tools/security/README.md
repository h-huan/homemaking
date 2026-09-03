# 提交前的凭据检查

使用 [Gitleaks 官方发布](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.1)的 8.30.1，按操作系统下载，并用同次发布的 checksums 文件核对 SHA-256。不要安装来历不明的扫描工具。扫描在本地进行，不会发送凭据到云服务验证。

在仓库根目录运行：

```powershell
git fetch origin saas-platform
./tools/security/check-secrets.ps1 -BaseRef origin/saas-platform -Gitleaks 'C:/工具目录/gitleaks.exe'
```

此脚本检查两部分：当前受 Git 管理的文件及未忽略的新文件；目标分支之后的待推送提交。输出使用 `--redact`，扫描失败会停止。仓库配置保留全部默认规则，并补充腾讯云、火山引擎标识规则；没有添加忽略凭据的白名单。它是手动提交前检查，未自动修改你的 Git hooks 或全局 Git 设置。

扫描通过不代表已经撤销暴露的密钥，也不代表检查了远端所有历史。现有根提交的遗留问题见下方「已知历史凭据」。需要审计完整分支时另外运行：

```powershell
gitleaks git . --config .gitleaks.toml --log-opts=saas-platform --redact --no-banner
```

遇到告警先移除真实值、换成空值/占位符或环境变量，并重写**尚未发布**的引入提交；只追加删除提交不够。已发布历史应先确认影响范围及仓库协作者，再进行独立历史清理。不要点击推送保护的 Bypass，不要把真实密钥放进扫描忽略清单。

真实配置只放服务器私有环境或配置管理中。被暴露且仍有效的凭据应由所属账号管理员撤销/轮换；不要拿上游示例凭据连接真实服务。新接入变量同时更新 [backend.env.example](../../deploy/backend.env.example) 与[变量清单](../../docs/deployment/environment-variables.md)。

## 已知历史凭据

迁移时触发推送保护的云存储标识及配套密钥来自锁定的上游提交 `ae176c856ac34291ecd3d3c89f0711eed39f9b49`，随 SQL 示例导入，并非用户为新平台提供的配置。整改已覆盖当前 SQL、可选模块/测试示例、legacy 配置及当时未发布的迁移提交；没有使用这些值连接真实服务。来源与许可证见 [THIRD-PARTY-NOTICES](../../THIRD-PARTY-NOTICES.md)。

迁移之前的共享根提交 `7156dcb` 仍有 7 处扫描告警，涉及旧数据库、Redis、微信配置和前端私钥；当前文件中的对应值已清理，但共享历史没有重写。若相关值曾实际使用，账号持有人须撤销或轮换。历史清理需要协调所有受影响分支与协作者，不能通过点击 Bypass 或添加扫描白名单掩盖问题。
