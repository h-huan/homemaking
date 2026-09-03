ALTER TABLE hm_worker_leave ADD COLUMN kind VARCHAR(12) NOT NULL DEFAULT 'LEAVE';
ALTER TABLE hm_worker_leave ADD COLUMN source VARCHAR(12) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE hm_worker_leave ADD COLUMN requested_by BIGINT NULL;
ALTER TABLE hm_worker_leave ADD COLUMN request_key VARCHAR(64) NULL;
ALTER TABLE hm_worker_leave ADD COLUMN request_hash CHAR(64) NULL;
ALTER TABLE hm_worker_leave ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE hm_worker_leave ADD COLUMN reviewed_by BIGINT NULL;
ALTER TABLE hm_worker_leave ADD COLUMN reviewed_at DATETIME NULL;
ALTER TABLE hm_worker_leave ADD COLUMN review_note VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE hm_worker_leave ADD COLUMN cancelled_by BIGINT NULL;
ALTER TABLE hm_worker_leave ADD COLUMN cancelled_at DATETIME NULL;
ALTER TABLE hm_worker_leave ADD COLUMN cancel_reason VARCHAR(500) NOT NULL DEFAULT '';
-- Historical rows keep unknown timestamps/operators; never fabricate their approval history.
ALTER TABLE hm_worker_leave ADD COLUMN created_at DATETIME NULL;
ALTER TABLE hm_worker_leave ADD COLUMN updated_at DATETIME NULL;
ALTER TABLE hm_worker_leave ADD CONSTRAINT uk_worker_leave_request UNIQUE(tenant_id,worker_id,request_key);
CREATE TABLE hm_worker_leave_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL,
 leave_id BIGINT NOT NULL, action VARCHAR(20) NOT NULL, from_status VARCHAR(20) NULL,
 to_status VARCHAR(20) NOT NULL, operator_id BIGINT NOT NULL, reason VARCHAR(500) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_worker_leave_log(tenant_id,worker_id,leave_id,id)
);
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(901120,'工作台 · 请假休息申请','homemaking:worker:leave',3,120,900006,'','','','',0,1,1,1,'migration','migration',FALSE),
(901121,'工作台 · 本人收入','homemaking:worker:income',3,121,900006,'','','','',0,1,1,1,'migration','migration',FALSE),
(901122,'排班 · 请假审批','homemaking:schedule:review',3,122,900004,'','','','',0,1,1,1,'migration','migration',FALSE);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,m.id,r.tenant_id,'migration','migration',FALSE FROM system_role r CROSS JOIN system_menu m
WHERE r.deleted=FALSE AND ((r.code='hm_worker' AND m.id IN (901120,901121))
 OR (r.code IN ('super_admin','tenant_admin','hm_owner','hm_manager') AND m.id=901122))
AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.tenant_id=r.tenant_id AND x.menu_id=m.id AND x.deleted=FALSE);
UPDATE system_tenant_package SET menu_ids=CONCAT(LEFT(RTRIM(menu_ids),CHAR_LENGTH(RTRIM(menu_ids))-1),',901120,901121]')
WHERE deleted=FALSE AND menu_ids REGEXP '(^|[^0-9])900006([^0-9]|$)';
UPDATE system_tenant_package SET menu_ids=CONCAT(LEFT(RTRIM(menu_ids),CHAR_LENGTH(RTRIM(menu_ids))-1),',901122]')
WHERE deleted=FALSE AND menu_ids REGEXP '(^|[^0-9])900004([^0-9]|$)';
INSERT INTO hm_schema_upgrade(version) VALUES('V008');
