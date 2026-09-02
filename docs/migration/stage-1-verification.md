# HM base verification — 2026-09-02

- Backend pin: ae176c856ac34291ecd3d3c89f0711eed39f9b49; frontend pin: e272d6639bcf87e0259bcb6ede721da4d047b2bc.
- Maven package: PASS, all 23 active reactor modules, Java 17.0.14; test classes compiled, tests not yet executed in this phase.
- pnpm 11.19.0 frozen dependency installation: PASS. esbuild and parcel watcher build hooks enabled; optional core-js/es5-ext postinstall hooks disabled.
- Vue production build: PASS. Full vue-tsc check: PASS after fixing upstream build mode to generate auto-import declarations.
- Application startup: PASS, `Started HmServerApplication in 28.118 seconds`.
- Smoke runtime: disposable H2 2.3.232 in MySQL compatibility mode, independent Redis on loopback port 16379 with a generated password. No existing business database was touched. MySQL-native acceptance remains a later gate; downloading the native test runtime failed with a transport EOF.
- Added pay/mp DDL from pinned source data objects because the upstream base SQL only contains system/infra. Existing business table prefixes retained.
- Active runtime configuration removes mock login, fixed SMS codes and demo provider secrets. File metadata is tenant scoped; downloads require authentication and tenant-owned metadata; server uploads inspect content types; direct uploads disabled pending verified upload-session handling.
- Optional backend modules are excluded from the reactor/application. Optional frontend route groups are preserved in `hm-ui/optional/remaining.ts` and excluded from active routes.

This phase validates the base. It does not claim business migration, real payment/WeChat delivery, production data migration or full security regression are complete.
