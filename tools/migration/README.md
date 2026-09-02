# 迁移映射回归

在仓库根目录执行（Java 17、Node、H2 2.3.232）：

```sh
node tools/migration/prepare-check.cjs
java -Dfile.encoding=UTF-8 -cp /path/to/h2-2.3.232.jar tools/migration/MigrationCheck.java
```

脚本从保留的旧 schema 创建合成数据，读取当前迁移 SQL；只在内存数据库运行。检查旧分类编号、订单金额、派单人员、预约占位、来源归档和重复导入拒绝。

适配器修改了 H2/MySQL 的 JSON_OBJECT、UPDATE JOIN、CAST CHAR 和事务语法，因此此结果**不能替代 MySQL 8.0.16+ 的原生全量迁移演练**。生产数据迁移流程见 `docs/migration/runbook.md`。
