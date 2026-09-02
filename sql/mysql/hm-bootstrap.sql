-- Fresh installation only. The account is locked until its password hash is set locally.
-- Internal login client only. No demo OAuth grants, redirects, or shared client secret.
INSERT INTO system_oauth2_client(id,client_id,secret,name,logo,status,access_token_validity_seconds,refresh_token_validity_seconds,redirect_uris,authorized_grant_types,scopes,auto_approve_scopes,authorities,resource_ids,additional_information,creator,updater,deleted)
VALUES(1,'default',REPLACE(UUID(),'-',''),'HM 家政','/hm-logo.svg',0,1800,2592000,'[]','[]','["homemaking"]','[]','[]','[]','{}','bootstrap','bootstrap',0);
INSERT INTO system_tenant (id, name, contact_name, contact_mobile, status, websites, package_id, expire_time, account_count, creator, updater, deleted)
VALUES (1, 'HM 总部', '总部管理员', '', 0, '["localhost","127.0.0.1"]', 0, '2099-12-31 23:59:59', 10000, 'bootstrap', 'bootstrap', 0);
INSERT INTO system_users (id, username, password, nickname, status, tenant_id, creator, updater, deleted)
VALUES (1, 'admin', '!BOOTSTRAP_DISABLED!', '总部管理员', 0, 1, 'bootstrap', 'bootstrap', 0);
INSERT INTO system_role (id, name, code, sort, data_scope, status, type, tenant_id, creator, updater, deleted)
VALUES (1, '总部管理员', 'super_admin', 1, 1, 0, 1, 1, 'bootstrap', 'bootstrap', 0);
INSERT INTO system_user_role (user_id, role_id, tenant_id, creator, updater, deleted)
VALUES (1, 1, 1, 'bootstrap', 'bootstrap', 0);
-- Optional modules remain in source and schema; their menus are disabled.
UPDATE system_menu SET status=1 WHERE parent_id=0 AND path NOT IN ('/system','/infra','/pay','/mp');
UPDATE system_menu SET status=1 WHERE component REGEXP '^pay/(demo|wallet)/';
UPDATE system_menu SET status = 1 WHERE component REGEXP '^(bpm|mall|member|crm|erp|ai|iot|mes|wms|hrm|fms|im|report)/'
 OR path IN ('bpm','mall','member','crm','erp','ai','iot','mes','wms','hrm','fms','im','report');
INSERT INTO system_role_menu (role_id, menu_id, tenant_id, creator, updater, deleted)
SELECT 1, id, 1, 'bootstrap', 'bootstrap', 0 FROM system_menu WHERE status = 0 AND deleted = 0;
