-- Apply after V002 on an existing HM installation. Safe to repeat if menus were already added by a fresh install.
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
VALUES
(900003,'官网内容','homemaking:manage',2,3,900000,'portal','ep:monitor','homemaking/Portal','HomemakingPortal',0,1,1,1,'migration','migration',0),
(900004,'排班中心','homemaking:manage',2,4,900000,'scheduling','ep:calendar','homemaking/Scheduling','HomemakingScheduling',0,1,1,1,'migration','migration',0),
(900005,'SaaS 与财务','homemaking:manage',2,5,900000,'finance','ep:coin','homemaking/Finance','HomemakingFinance',0,1,1,1,'migration','migration',0),
(900006,'服务人员工作台','homemaking:worker',2,6,900000,'worker','ep:user','homemaking/Worker','HomemakingWorker',0,1,1,1,'migration','migration',0)
ON DUPLICATE KEY UPDATE name=VALUES(name),permission=VALUES(permission),sort=VALUES(sort),path=VALUES(path),icon=VALUES(icon),component=VALUES(component),component_name=VALUES(component_name),status=VALUES(status),visible=VALUES(visible);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT 1,m.id,1,'migration','migration',0 FROM system_menu m
WHERE m.id IN (900003,900004,900005,900006)
AND NOT EXISTS (SELECT 1 FROM system_role_menu r WHERE r.role_id=1 AND r.menu_id=m.id AND r.tenant_id=1 AND r.deleted=0);
INSERT INTO hm_schema_upgrade(version) VALUES('V003') ON DUPLICATE KEY UPDATE version=VALUES(version);
