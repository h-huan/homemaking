-- Apply after V003. Existing broad grants do not automatically acquire the new permissions.
CREATE TABLE hm_staff_scope (
 tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, template_code VARCHAR(20) NOT NULL,
 updated_by BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,user_id), CHECK(template_code IN ('PLATFORM','OWNER','MANAGER','DISPATCHER','SUPPORT','FINANCE','WORKER'))
);
CREATE TABLE hm_staff_store (
 tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, store_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,user_id,store_id), KEY idx_staff_store(tenant_id,store_id)
);
CREATE TABLE hm_staff_access_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 template_code VARCHAR(20) NOT NULL, store_ids VARCHAR(5000) NOT NULL, operator_id BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_staff_audit(tenant_id,user_id,id)
);
UPDATE system_menu SET permission='homemaking:operations:read' WHERE id=900001;
UPDATE system_menu SET permission='homemaking:brand:read' WHERE id=900002;
UPDATE system_menu SET permission='homemaking:portal:read' WHERE id=900003;
UPDATE system_menu SET permission='homemaking:schedule:read' WHERE id=900004;
UPDATE system_menu SET permission='homemaking:finance:read' WHERE id=900005;
UPDATE system_menu SET permission='homemaking:worker:read' WHERE id=900006;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
VALUES(900007,'人员权限','homemaking:staff:read',2,7,900000,'staff','ep:lock','homemaking/Staff','HomemakingStaff',0,1,1,1,'migration','migration',0);
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(901000,'门店 · 查看','homemaking:stores:read',3,0,900001,'','','','',0,1,1,1,'migration','migration',0),
(901001,'门店 · 编辑','homemaking:stores:write',3,1,900001,'','','','',0,1,1,1,'migration','migration',0),
(901002,'人员 · 查看','homemaking:workers:read',3,2,900001,'','','','',0,1,1,1,'migration','migration',0),
(901003,'人员 · 编辑','homemaking:workers:write',3,3,900001,'','','','',0,1,1,1,'migration','migration',0),
(901004,'人员 · 绑定账号','homemaking:workers:bind',3,4,900001,'','','','',0,1,1,1,'migration','migration',0),
(901005,'服务 · 查看','homemaking:services:read',3,5,900001,'','','','',0,1,1,1,'migration','migration',0),
(901006,'服务 · 编辑','homemaking:services:write',3,6,900001,'','','','',0,1,1,1,'migration','migration',0),
(901007,'订单 · 查看','homemaking:orders:read',3,7,900001,'','','','',0,1,1,1,'migration','migration',0),
(901008,'订单 · 派单','homemaking:orders:dispatch',3,8,900001,'','','','',0,1,1,1,'migration','migration',0),
(901009,'订单 · 改期','homemaking:orders:reschedule',3,9,900001,'','','','',0,1,1,1,'migration','migration',0),
(901010,'订单 · 取消','homemaking:orders:cancel',3,10,900001,'','','','',0,1,1,1,'migration','migration',0),
(901011,'订单 · 履约','homemaking:orders:fulfill',3,11,900001,'','','','',0,1,1,1,'migration','migration',0),
(901012,'客户 · 查看','homemaking:customers:read',3,12,900001,'','','','',0,1,1,1,'migration','migration',0),
(901013,'售后 · 查看','homemaking:aftersales:read',3,13,900001,'','','','',0,1,1,1,'migration','migration',0),
(901014,'售后 · 驳回','homemaking:aftersales:reject',3,14,900001,'','','','',0,1,1,1,'migration','migration',0),
(901015,'售后 · 退款','homemaking:aftersales:refund',3,15,900001,'','','','',0,1,1,1,'migration','migration',0),
(901016,'评价 · 查看','homemaking:reviews:read',3,16,900001,'','','','',0,1,1,1,'migration','migration',0),
(901017,'评价 · 审核','homemaking:reviews:moderate',3,17,900001,'','','','',0,1,1,1,'migration','migration',0),
(901018,'排班 · 查看','homemaking:schedule:read',3,18,900004,'','','','',0,1,1,1,'migration','migration',0),
(901019,'排班 · 编辑','homemaking:schedule:write',3,19,900004,'','','','',0,1,1,1,'migration','migration',0),
(901020,'排班 · 班次模板','homemaking:schedule:templates',3,20,900004,'','','','',0,1,1,1,'migration','migration',0),
(901021,'财务 · 查看','homemaking:finance:read',3,21,900005,'','','','',0,1,1,1,'migration','migration',0),
(901022,'财务 · 生成结算','homemaking:finance:statement',3,22,900005,'','','','',0,1,1,1,'migration','migration',0),
(901023,'财务 · 审核结算','homemaking:finance:approve',3,23,900005,'','','','',0,1,1,1,'migration','migration',0),
(901024,'财务 · 登记打款','homemaking:finance:payout',3,24,900005,'','','','',0,1,1,1,'migration','migration',0),
(901025,'财务 · 对账','homemaking:finance:reconcile',3,25,900005,'','','','',0,1,1,1,'migration','migration',0),
(901026,'品牌 · 查看','homemaking:brand:read',3,26,900002,'','','','',0,1,1,1,'migration','migration',0),
(901027,'品牌 · 编辑','homemaking:brand:write',3,27,900002,'','','','',0,1,1,1,'migration','migration',0),
(901028,'通知 · 查看','homemaking:notification:read',3,28,900002,'','','','',0,1,1,1,'migration','migration',0),
(901029,'通知 · 编辑','homemaking:notification:write',3,29,900002,'','','','',0,1,1,1,'migration','migration',0),
(901030,'官网 · 查看','homemaking:portal:read',3,30,900003,'','','','',0,1,1,1,'migration','migration',0),
(901031,'官网 · 编辑','homemaking:portal:write',3,31,900003,'','','','',0,1,1,1,'migration','migration',0),
(901032,'官网 · 发布','homemaking:portal:publish',3,32,900003,'','','','',0,1,1,1,'migration','migration',0),
(901033,'人员权限 · 查看','homemaking:staff:read',3,33,900007,'','','','',0,1,1,1,'migration','migration',0),
(901034,'人员权限 · 编辑','homemaking:staff:write',3,34,900007,'','','','',0,1,1,1,'migration','migration',0),
(901035,'配额 · 查看','homemaking:quota:read',3,35,900005,'','','','',0,1,1,1,'migration','migration',0),
(901036,'平台 · 管理','homemaking:platform:manage',3,36,900005,'','','','',0,1,1,1,'migration','migration',0),
(901037,'工作台 · 查看','homemaking:worker:read',3,37,900006,'','','','',0,1,1,1,'migration','migration',0),
(901038,'工作台 · 履约','homemaking:worker:fulfill',3,38,900006,'','','','',0,1,1,1,'migration','migration',0);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,m.id,r.tenant_id,'migration','migration',FALSE FROM system_role r CROSS JOIN system_menu m
WHERE r.code='super_admin' AND r.deleted=FALSE AND (m.id=900007 OR m.id BETWEEN 901000 AND 901999)
AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.menu_id=m.id AND x.tenant_id=r.tenant_id AND x.deleted=FALSE);
INSERT INTO hm_schema_upgrade(version) VALUES('V004');
