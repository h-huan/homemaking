INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
VALUES
(900000,'家政运营','',1,1,0,'/homemaking','ep:house','','',0,1,1,1,'migration','migration',0),
(900001,'运营工作台','homemaking:manage',2,1,900000,'operations','ep:calendar','homemaking/Operations','HomemakingOperations',0,1,1,1,'migration','migration',0),
(900002,'品牌与通知','homemaking:manage',2,2,900000,'settings','ep:setting','homemaking/Settings','HomemakingSettings',0,1,1,1,'migration','migration',0),
(900003,'官网内容','homemaking:manage',2,3,900000,'portal','ep:monitor','homemaking/Portal','HomemakingPortal',0,1,1,1,'migration','migration',0),
(900004,'排班中心','homemaking:manage',2,4,900000,'scheduling','ep:calendar','homemaking/Scheduling','HomemakingScheduling',0,1,1,1,'migration','migration',0),
(900005,'SaaS 与财务','homemaking:manage',2,5,900000,'finance','ep:coin','homemaking/Finance','HomemakingFinance',0,1,1,1,'migration','migration',0),
(900006,'服务人员工作台','homemaking:worker',2,6,900000,'worker','ep:user','homemaking/Worker','HomemakingWorker',0,1,1,1,'migration','migration',0);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
VALUES(1,900000,1,'migration','migration',0),(1,900001,1,'migration','migration',0),(1,900002,1,'migration','migration',0),
      (1,900003,1,'migration','migration',0),(1,900004,1,'migration','migration',0),(1,900005,1,'migration','migration',0),(1,900006,1,'migration','migration',0);
INSERT INTO hm_tenant_domain(domain,tenant_id,verification_token,verified)
VALUES('localhost',1,'local-development',TRUE),('127.0.0.1',1,'local-development',TRUE);
