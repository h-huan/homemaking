-- Platform authority is explicit and global; account_tenant_id is only the account's login partition.
CREATE TABLE system_platform_operator (
 user_id BIGINT PRIMARY KEY, account_tenant_id BIGINT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 1, updated_by BIGINT NULL, reason VARCHAR(500) NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_platform_account(account_tenant_id,user_id)
);
CREATE TABLE system_platform_access_lock (id BIGINT PRIMARY KEY);
INSERT INTO system_platform_access_lock(id) VALUES(0);
CREATE TABLE system_platform_access_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, action VARCHAR(20) NOT NULL, operator_id BIGINT NOT NULL,
 account_tenant_id BIGINT NOT NULL, target_tenant_id BIGINT NOT NULL, target_user_id BIGINT NULL,
 before_value VARCHAR(1000) NULL, after_value VARCHAR(1000) NULL, reason VARCHAR(500) NOT NULL,
 request_ip VARCHAR(64) NOT NULL DEFAULT '', request_method VARCHAR(12) NOT NULL DEFAULT '',
 request_path VARCHAR(500) NOT NULL DEFAULT '', response_status INT NOT NULL DEFAULT 200,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_platform_audit(operator_id,id), KEY idx_platform_target(target_tenant_id,id)
);

-- One-time compatibility conversion of the PREVIOUS authorization rule. This is not a runtime rule.
-- Preserve only accounts that already held platform authority; tenant membership alone grants nothing.
INSERT INTO system_platform_operator(user_id,account_tenant_id,enabled,version,reason)
SELECT DISTINCT u.id,u.tenant_id,TRUE,1,'迁移既有平台授权'
FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id
JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id
WHERE u.tenant_id=1 AND u.status=0 AND u.deleted=FALSE AND ur.deleted=FALSE
 AND r.deleted=FALSE AND r.status=0 AND r.code IN ('super_admin','hm_platform');
INSERT INTO system_platform_access_log(action,operator_id,account_tenant_id,target_tenant_id,target_user_id,before_value,after_value,reason)
SELECT 'MIGRATE',user_id,account_tenant_id,account_tenant_id,user_id,'legacy platform role','true','迁移既有平台授权'
FROM system_platform_operator;

-- The platform ledger beneficiary uses a non-tenant identifier. Business tenant predicates remain intact.
UPDATE hm_settlement_entry SET beneficiary_id=0 WHERE beneficiary='PLATFORM';
UPDATE hm_staff_scope SET template_code='OWNER' WHERE template_code='PLATFORM';

INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
VALUES(900008,'平台身份','system:platform-access:manage',2,8,900000,'platform-access','ep:key','homemaking/PlatformAccess','HomemakingPlatformAccess',0,1,0,1,'migration','migration',0);

-- Headquarters becomes an ordinary business tenant with an ordinary, editable package.
-- JSON_ARRAYAGG avoids GROUP_CONCAT truncation; only active business menus are included.
ALTER TABLE system_tenant_package MODIFY COLUMN menu_ids TEXT NOT NULL;
INSERT INTO system_tenant_package(name,status,remark,menu_ids,creator,updater,deleted)
SELECT 'HM 直营业务',0,'直营业务套餐；不授予平台身份',JSON_ARRAYAGG(id),'migration','migration',FALSE
FROM system_menu WHERE deleted=FALSE AND status=0
 AND permission NOT LIKE 'system:tenant:%' AND permission NOT LIKE 'system:tenant-package:%'
 AND permission NOT LIKE 'system:platform-access:%' AND permission NOT LIKE 'homemaking:platform:%'
 AND permission NOT LIKE 'system:menu:%' AND permission NOT LIKE 'system:dict-%'
 AND permission NOT LIKE 'system:sms-%' AND permission NOT LIKE 'system:mail-%'
 AND permission NOT LIKE 'system:notify-template:%' AND permission NOT LIKE 'system:oauth2-client:%'
 AND permission NOT LIKE 'infra:file-config:%' AND permission NOT LIKE 'infra:data-source-config:%';
UPDATE system_tenant SET package_id=(SELECT MAX(id) FROM system_tenant_package WHERE name='HM 直营业务' AND deleted=FALSE)
WHERE package_id=0;
INSERT INTO hm_schema_upgrade(version) VALUES('V007');
