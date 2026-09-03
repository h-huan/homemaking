-- Apply once after V005. Existing financial receipts are never overwritten.
ALTER TABLE hm_order ADD COLUMN area_fee_cents INT NULL;
ALTER TABLE hm_order ADD COLUMN pending_change_id BIGINT NULL;
ALTER TABLE hm_aftersale ADD COLUMN order_change_id BIGINT NULL;
ALTER TABLE hm_payment_entry ADD COLUMN order_change_id BIGINT NULL;
CREATE TABLE hm_order_change (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 kind VARCHAR(16) NOT NULL, status VARCHAR(24) NOT NULL,
 old_price_cents INT NOT NULL, new_price_cents INT NOT NULL, difference_cents INT NOT NULL,
 before_data TEXT NOT NULL, after_data TEXT NOT NULL, reason VARCHAR(500) NOT NULL,
 actor_id BIGINT NOT NULL, actor_type INT NOT NULL, request_key VARCHAR(100) NOT NULL, request_hash VARCHAR(64) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, applied_at TIMESTAMP NULL,
 cancelled_by BIGINT NULL, cancelled_type INT NULL, cancel_reason VARCHAR(500) NOT NULL DEFAULT '', cancelled_at TIMESTAMP NULL,
 UNIQUE KEY uk_order_change_request(tenant_id,order_id,request_key), KEY idx_order_change(tenant_id,order_id,id),
 CHECK(kind IN ('AMENDMENT','RESCHEDULE')),
 CHECK(status IN ('PENDING_PAYMENT','PENDING_REFUND','APPLIED','CANCELLED')),
 CHECK(old_price_cents>0 AND new_price_cents>0)
);
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(901110,'订单 · 变更地址','homemaking:orders:change',3,110,900001,'','','','',0,1,1,1,'migration','migration',FALSE),
(901111,'订单 · 调整价格','homemaking:orders:price',3,111,900001,'','','','',0,1,1,1,'migration','migration',FALSE);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,m.id,r.tenant_id,'migration','migration',FALSE FROM system_role r CROSS JOIN system_menu m
WHERE r.deleted=FALSE AND m.id IN (901110,901111) AND
 (r.code IN ('super_admin','hm_platform','hm_owner','hm_manager') OR (r.code='hm_support' AND m.id=901110))
AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.menu_id=m.id AND x.tenant_id=r.tenant_id AND x.deleted=FALSE);
INSERT INTO hm_schema_upgrade(version) VALUES('V006');
