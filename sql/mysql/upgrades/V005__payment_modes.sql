-- Apply once after V004. Merchant credentials remain in the existing pay module.
ALTER TABLE hm_tenant_profile ADD COLUMN payment_mode VARCHAR(10) NOT NULL DEFAULT 'OFFLINE';
ALTER TABLE hm_tenant_profile ADD CONSTRAINT chk_hm_payment_mode CHECK(payment_mode IN ('OFFLINE','ONLINE','BOTH'));
ALTER TABLE hm_order ADD COLUMN payment_method VARCHAR(16) NOT NULL DEFAULT 'OFFLINE';
ALTER TABLE hm_order ADD COLUMN payment_expires_at DATETIME NULL;
UPDATE hm_order SET payment_method='ONLINE',payment_expires_at=created_at + INTERVAL '30' MINUTE WHERE pay_order_id IS NOT NULL;
UPDATE hm_order SET payment_method='LEGACY' WHERE pay_order_id IS NULL AND paid_cents>0;
CREATE TABLE hm_payment_entry (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 aftersale_id BIGINT NULL, reversal_of BIGINT NULL, kind VARCHAR(12) NOT NULL,
 payment_method VARCHAR(16) NOT NULL, channel VARCHAR(24) NOT NULL, amount_cents INT NOT NULL,
 occurred_at DATETIME NOT NULL, operator_id BIGINT NULL, note VARCHAR(1000) NOT NULL DEFAULT '',
 request_key VARCHAR(100) NOT NULL, recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_payment_request(tenant_id,request_key), UNIQUE KEY uk_payment_refund(tenant_id,aftersale_id),
 UNIQUE KEY uk_payment_reversal(tenant_id,reversal_of), KEY idx_payment_order(tenant_id,order_id,id),
 KEY idx_payment_time(tenant_id,occurred_at),
 CHECK(kind IN ('RECEIPT','REFUND','REVERSAL')),
 CHECK((kind='RECEIPT' AND amount_cents>0) OR (kind IN ('REFUND','REVERSAL') AND amount_cents<0)),
 CHECK(payment_method IN ('OFFLINE','ONLINE')),
 CHECK((payment_method='ONLINE' AND channel='WX_MINI') OR (payment_method='OFFLINE' AND channel IN ('CASH','WECHAT_TRANSFER','ALIPAY_TRANSFER','BANK_TRANSFER','OTHER')))
);
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(901100,'收款 · 查看','homemaking:payment:read',3,100,900001,'','','','',0,1,1,1,'migration','migration',FALSE),
(901101,'收款 · 确认','homemaking:payment:receive',3,101,900001,'','','','',0,1,1,1,'migration','migration',FALSE),
(901102,'收款 · 冲正','homemaking:payment:reverse',3,102,900001,'','','','',0,1,1,1,'migration','migration',FALSE),
(901103,'支付模式 · 配置','homemaking:payment:configure',3,103,900002,'','','','',0,1,1,1,'migration','migration',FALSE);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,m.id,r.tenant_id,'migration','migration',FALSE FROM system_role r CROSS JOIN system_menu m
WHERE r.deleted=FALSE AND m.id BETWEEN 901100 AND 901103 AND
 (r.code IN ('super_admin','hm_platform','hm_owner') OR (r.code='hm_finance' AND m.id<=901102) OR (r.code='hm_manager' AND m.id<=901101))
AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.menu_id=m.id AND x.tenant_id=r.tenant_id AND x.deleted=FALSE);
INSERT INTO hm_schema_upgrade(version) VALUES('V005');
