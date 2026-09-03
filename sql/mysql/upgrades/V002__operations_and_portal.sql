-- Apply ONCE after the five HM initialization scripts, or to an existing 502a15e database.
-- Back up first. Do not rerun hm-base.sql. New workers require explicit skills and schedules.
CREATE TABLE hm_schema_upgrade (version VARCHAR(32) PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE hm_worker_skill (
 tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL, service_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,worker_id,service_id)
);
CREATE TABLE hm_worker_area (
 tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL, district_code VARCHAR(20) NOT NULL,
 PRIMARY KEY(tenant_id,worker_id,district_code)
);
CREATE TABLE hm_shift_template (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, name VARCHAR(100) NOT NULL,
 starts_at TIME NOT NULL, ends_at TIME NOT NULL, KEY idx_shift_tenant(tenant_id), CHECK(starts_at<ends_at)
);
CREATE TABLE hm_worker_schedule (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL,
 starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL,
 KEY idx_schedule_worker(tenant_id,worker_id,starts_at), CHECK(starts_at<ends_at)
);
CREATE TABLE hm_worker_leave (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL,
 starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL, reason VARCHAR(500) NOT NULL DEFAULT '',
 status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', KEY idx_leave_worker(tenant_id,worker_id,starts_at), CHECK(starts_at<ends_at)
);
CREATE TABLE hm_worker_account (
 tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,worker_id), UNIQUE KEY uk_worker_account(tenant_id,user_id)
);
ALTER TABLE hm_order ADD COLUMN district_code VARCHAR(20) NOT NULL DEFAULT '';
ALTER TABLE hm_order ADD COLUMN fulfillment_status VARCHAR(20) NOT NULL DEFAULT 'WAITING';
ALTER TABLE hm_order ADD COLUMN reschedule_count INT NOT NULL DEFAULT 0;
ALTER TABLE hm_review ADD COLUMN visible BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE hm_aftersale ADD COLUMN audit_remark VARCHAR(1000) NOT NULL DEFAULT '';
CREATE TABLE hm_fulfillment_evidence (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 worker_id BIGINT NOT NULL, storage_key VARCHAR(200) NOT NULL, content_type VARCHAR(40) NOT NULL, phase VARCHAR(10) NOT NULL,
 note VARCHAR(500) NOT NULL DEFAULT '', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_evidence_file(storage_key), KEY idx_evidence_order(tenant_id,order_id)
);
CREATE TABLE hm_saas_plan (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, limits_json TEXT NOT NULL,
 features_json TEXT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE hm_tenant_entitlement (
 tenant_id BIGINT PRIMARY KEY, plan_id BIGINT, limits_json TEXT NOT NULL, features_json TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE hm_usage_counter (
 tenant_id BIGINT NOT NULL, resource VARCHAR(30) NOT NULL, period VARCHAR(10) NOT NULL, used BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,resource,period), CHECK(used>=0)
);
CREATE TABLE hm_commission_rule (
 tenant_id BIGINT PRIMARY KEY, platform_bps INT NOT NULL DEFAULT 0, worker_bps INT NOT NULL DEFAULT 0,
 cycle_days INT NOT NULL DEFAULT 30, enabled BOOLEAN NOT NULL DEFAULT FALSE, version BIGINT NOT NULL DEFAULT 0,
 CHECK(platform_bps>=0 AND worker_bps>=0 AND platform_bps+worker_bps<=10000), CHECK(cycle_days BETWEEN 1 AND 90)
);
CREATE TABLE hm_settlement_allocation (
 tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL, store_id BIGINT NOT NULL, worker_id BIGINT,
 platform_bps INT NOT NULL, worker_bps INT NOT NULL, net_cents BIGINT NOT NULL DEFAULT 0,
 platform_cents BIGINT NOT NULL DEFAULT 0, worker_cents BIGINT NOT NULL DEFAULT 0, store_cents BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,order_id)
);
CREATE TABLE hm_settlement_statement (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, beneficiary VARCHAR(20) NOT NULL, beneficiary_id BIGINT NOT NULL,
 period_start DATE NOT NULL, period_end DATE NOT NULL, amount_cents BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', created_by BIGINT, approved_by BIGINT,
 payment_reference VARCHAR(150), paid_by BIGINT, paid_at DATETIME, reconciled_by BIGINT, reconciled_at DATETIME,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_statement_payment(tenant_id,payment_reference), KEY idx_statement_tenant(tenant_id,status)
);
CREATE TABLE hm_settlement_entry (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 beneficiary VARCHAR(20) NOT NULL, beneficiary_id BIGINT NOT NULL, amount_cents BIGINT NOT NULL,
 event_key VARCHAR(100) NOT NULL, statement_id BIGINT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_ledger_event(tenant_id,event_key,beneficiary,beneficiary_id), KEY idx_unsettled(tenant_id,statement_id,created_at)
);
CREATE TABLE hm_portal_site (
 tenant_id BIGINT PRIMARY KEY, draft_json LONGTEXT NOT NULL, published_json LONGTEXT,
 version BIGINT NOT NULL DEFAULT 0, published_version BIGINT, published_at DATETIME, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE hm_portal_revision (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, version BIGINT NOT NULL,
 config_json LONGTEXT NOT NULL, published_by BIGINT, published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_portal_revision(tenant_id,version)
);
INSERT INTO hm_schema_upgrade(version) VALUES('V002');
