-- Apply once after V011. Franchise records are platform data linked to, but not stored in, system_tenant.
CREATE TABLE hm_franchisee (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, code VARCHAR(40) NOT NULL,
 company_name VARCHAR(160) NOT NULL, credit_code VARCHAR(32) NOT NULL DEFAULT '', leader_name VARCHAR(80) NOT NULL,
 leader_mobile_ciphertext VARCHAR(500) NOT NULL, leader_mobile_last4 CHAR(4) NOT NULL,
 contact_email VARCHAR(160) NOT NULL DEFAULT '', registered_address VARCHAR(500) NOT NULL DEFAULT '',
 settlement_account_name VARCHAR(160) NOT NULL, settlement_bank_name VARCHAR(160) NOT NULL,
 settlement_account_ciphertext VARCHAR(500) NOT NULL, settlement_account_last4 VARCHAR(4) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', version BIGINT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL, updated_by BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 terminated_by BIGINT NULL, terminated_at DATETIME NULL, termination_reason VARCHAR(500) NOT NULL DEFAULT '',
 UNIQUE KEY uk_franchise_tenant(tenant_id), UNIQUE KEY uk_franchise_code(code),
 KEY idx_franchise_status(status,id),
 CONSTRAINT chk_franchise_status CHECK(status IN ('PENDING','ACTIVE','SUSPENDED','TERMINATED'))
);
CREATE TABLE hm_franchise_region (
 franchisee_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, region_code VARCHAR(32) NOT NULL,
 region_name VARCHAR(100) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(franchisee_id,region_code), KEY idx_franchise_region_tenant(tenant_id,region_code)
);
CREATE TABLE hm_franchise_contract (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, franchisee_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL,
 contract_no VARCHAR(60) NOT NULL, signed_on DATE NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
 deposit_cents BIGINT NOT NULL DEFAULT 0, deposit_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', previous_contract_id BIGINT NULL,
 remark VARCHAR(1000) NOT NULL DEFAULT '', version BIGINT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL, updated_by BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 activated_by BIGINT NULL, activated_at DATETIME NULL, terminated_by BIGINT NULL, terminated_at DATETIME NULL,
 termination_reason VARCHAR(500) NOT NULL DEFAULT '',
 UNIQUE KEY uk_franchise_contract_no(contract_no), KEY idx_franchise_contract(franchisee_id,status,start_date,end_date),
 KEY idx_franchise_contract_tenant(tenant_id,status,end_date),
 CONSTRAINT chk_franchise_contract_dates CHECK(end_date>=start_date),
 CONSTRAINT chk_franchise_contract_deposit CHECK(deposit_cents>=0),
 CONSTRAINT chk_franchise_contract_status CHECK(status IN ('DRAFT','ACTIVE','EXPIRED','TERMINATED')),
 CONSTRAINT chk_franchise_deposit_status CHECK(deposit_status IN ('NOT_REQUIRED','PENDING','PAID','REFUNDED','FORFEITED'))
);
CREATE TABLE hm_franchise_deposit_entry (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, franchisee_id BIGINT NOT NULL, contract_id BIGINT NOT NULL,
 tenant_id BIGINT NOT NULL, entry_type VARCHAR(20) NOT NULL, amount_cents BIGINT NOT NULL,
 channel VARCHAR(24) NOT NULL, occurred_at DATETIME NOT NULL, reference_no VARCHAR(100) NOT NULL DEFAULT '',
 request_key VARCHAR(80) NOT NULL, operator_id BIGINT NOT NULL, note VARCHAR(500) NOT NULL DEFAULT '',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_franchise_deposit_request(contract_id,request_key),
 KEY idx_franchise_deposit(contract_id,id), KEY idx_franchise_deposit_tenant(tenant_id,occurred_at),
 CONSTRAINT chk_franchise_deposit_type CHECK(entry_type IN ('RECEIPT','REFUND','FORFEIT')),
 CONSTRAINT chk_franchise_deposit_amount CHECK(amount_cents>0)
);
CREATE TABLE hm_franchise_audit_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, action VARCHAR(40) NOT NULL, franchisee_id BIGINT NOT NULL,
 contract_id BIGINT NULL, tenant_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 before_json TEXT NOT NULL, after_json TEXT NOT NULL, reason VARCHAR(500) NOT NULL DEFAULT '',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_franchise_audit(franchisee_id,id), KEY idx_franchise_audit_tenant(tenant_id,id)
);
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(900009,'加盟与合同','homemaking:franchise:read',2,9,900000,'franchise','ep:office-building','homemaking/Franchise','HomemakingFranchise',0,1,1,1,'migration','migration',FALSE),
(901150,'加盟档案维护','homemaking:franchise:write',3,150,900009,'','','','',0,1,1,1,'migration','migration',FALSE),
(901151,'加盟合同维护','homemaking:franchise:contract',3,151,900009,'','','','',0,1,1,1,'migration','migration',FALSE),
(901152,'加盟结算资料','homemaking:franchise:settlement',3,152,900009,'','','','',0,1,1,1,'migration','migration',FALSE);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,m.id,r.tenant_id,'migration','migration',FALSE FROM system_role r CROSS JOIN system_menu m
WHERE r.deleted=FALSE AND r.code IN ('super_admin','hm_platform') AND m.id IN (900009,901150,901151,901152)
AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.tenant_id=r.tenant_id AND x.menu_id=m.id AND x.deleted=FALSE);
INSERT INTO hm_schema_upgrade(version) VALUES('V012');
