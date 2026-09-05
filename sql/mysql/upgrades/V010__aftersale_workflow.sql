-- Apply once after V009. Existing aftersales remain refund requests.
ALTER TABLE hm_aftersale MODIFY COLUMN amount_cents INT NULL;
ALTER TABLE hm_aftersale ADD COLUMN type VARCHAR(24) NOT NULL DEFAULT 'PARTIAL_REFUND';
ALTER TABLE hm_aftersale ADD COLUMN request_key VARCHAR(100) NULL;
ALTER TABLE hm_aftersale ADD COLUMN resolution VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE hm_aftersale ADD COLUMN assigned_worker_id BIGINT NULL;
ALTER TABLE hm_aftersale ADD COLUMN remedy_booking_id BIGINT NULL;
ALTER TABLE hm_aftersale ADD COLUMN scheduled_at DATETIME NULL;
ALTER TABLE hm_aftersale ADD COLUMN processed_at DATETIME NULL;
ALTER TABLE hm_aftersale ADD COLUMN closed_at DATETIME NULL;
ALTER TABLE hm_aftersale ADD COLUMN refund_attempt INT NOT NULL DEFAULT 0;
ALTER TABLE hm_aftersale ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE hm_aftersale ADD CONSTRAINT chk_aftersale_type CHECK(type IN ('REWORK','REASSIGN_WORKER','REVISIT','PARTIAL_REFUND','FULL_REFUND','OTHER_COMPENSATION'));
ALTER TABLE hm_aftersale ADD CONSTRAINT chk_aftersale_status CHECK(status IN ('REQUESTED','SCHEDULED','IN_PROGRESS','AWAITING_CONFIRMATION','REFUNDING','REFUNDED','COMPLETED','REJECTED','CANCELLED','FAILED','MIGRATION_REVIEW'));
ALTER TABLE hm_aftersale ADD CONSTRAINT chk_aftersale_amount CHECK((type IN ('PARTIAL_REFUND','FULL_REFUND') AND amount_cents>0) OR (type IN ('REWORK','REASSIGN_WORKER','REVISIT','OTHER_COMPENSATION') AND amount_cents IS NULL));
CREATE UNIQUE INDEX uk_aftersale_request ON hm_aftersale(tenant_id,customer_id,request_key);
ALTER TABLE hm_booking ADD COLUMN aftersale_id BIGINT NULL;
CREATE UNIQUE INDEX uk_booking_aftersale ON hm_booking(tenant_id,aftersale_id);
CREATE TABLE hm_aftersale_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, aftersale_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL, from_status VARCHAR(30) NULL, to_status VARCHAR(30) NOT NULL,
 actor_id BIGINT NULL, actor_type INT NULL, detail VARCHAR(1000) NOT NULL DEFAULT '', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_aftersale_log(tenant_id,aftersale_id,id), KEY idx_aftersale_order_log(tenant_id,order_id,id)
);
INSERT INTO hm_aftersale_log(tenant_id,aftersale_id,order_id,action,from_status,to_status,detail,created_at)
 SELECT tenant_id,id,order_id,'MIGRATED',NULL,status,'升级前售后记录',created_at FROM hm_aftersale;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(901130,'售后 · 处理服务补救','homemaking:aftersales:process',3,130,900001,'','','','',0,1,1,1,'migration','migration',FALSE);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,901130,r.tenant_id,'migration','migration',FALSE FROM system_role r
WHERE r.deleted=FALSE AND r.code IN ('super_admin','hm_platform','hm_owner','hm_manager','hm_support')
AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.menu_id=901130 AND x.tenant_id=r.tenant_id AND x.deleted=FALSE);
INSERT INTO hm_schema_upgrade(version) VALUES('V010');
