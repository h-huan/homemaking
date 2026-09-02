-- Apply once to the fresh HM database, after hm-base.sql and hm-bootstrap.sql.
-- Money is stored in integer cents. Platform identities never contain tenant-local business data.
CREATE TABLE hm_customer (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, mobile VARCHAR(32), nickname VARCHAR(128) NOT NULL DEFAULT '', avatar VARCHAR(1000) NOT NULL DEFAULT '',
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE hm_customer_tenant (
 tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 source VARCHAR(30) NOT NULL DEFAULT 'WECHAT', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,customer_id), FOREIGN KEY(customer_id) REFERENCES hm_customer(id)
);
CREATE TABLE hm_wechat_union (
 platform_id VARCHAR(100) NOT NULL, union_id VARCHAR(200) NOT NULL, customer_id BIGINT NOT NULL,
 PRIMARY KEY(platform_id,union_id), FOREIGN KEY(customer_id) REFERENCES hm_customer(id)
);
CREATE TABLE hm_wechat_app (
 tenant_id BIGINT NOT NULL, app_id VARCHAR(100) NOT NULL, kind VARCHAR(10) NOT NULL, platform_id VARCHAR(100) NOT NULL,
 secret_env VARCHAR(150) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(app_id), KEY idx_tenant(tenant_id)
);
CREATE TABLE hm_wechat_identity (
 app_id VARCHAR(100) NOT NULL, open_id VARCHAR(200) NOT NULL, customer_id BIGINT NOT NULL, union_id VARCHAR(200),
 subscribed BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(app_id,open_id), UNIQUE KEY uk_app_customer(app_id,customer_id), FOREIGN KEY(customer_id) REFERENCES hm_customer(id)
);
CREATE TABLE hm_tenant_profile (
 tenant_id BIGINT PRIMARY KEY, operation_mode VARCHAR(20) NOT NULL DEFAULT 'DIRECT', brand_name VARCHAR(128) NOT NULL DEFAULT 'HM 家政',
 website VARCHAR(1000) NOT NULL DEFAULT '', logo VARCHAR(1000) NOT NULL DEFAULT '/hm-logo.svg', favicon VARCHAR(1000) NOT NULL DEFAULT '/hm-logo.svg',
 primary_color VARCHAR(7) NOT NULL DEFAULT '#136f63', login_title VARCHAR(200) NOT NULL DEFAULT '欢迎使用 HM 家政',
 login_background VARCHAR(1000) NOT NULL DEFAULT '', home_modules TEXT NOT NULL,
 mini_app_id VARCHAR(100) NOT NULL DEFAULT '', mp_app_id VARCHAR(100) NOT NULL DEFAULT '', pay_app_key VARCHAR(100) NOT NULL DEFAULT '',
 version BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO hm_tenant_profile(tenant_id,brand_name,home_modules) VALUES(1,'HM 总部','["services","stores","workers"]');
CREATE TABLE hm_tenant_domain (
 domain VARCHAR(253) PRIMARY KEY, tenant_id BIGINT NOT NULL, verification_token VARCHAR(100) NOT NULL,
 verified BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_domain_tenant(tenant_id)
);
CREATE TABLE hm_store (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, address VARCHAR(500) NOT NULL DEFAULT '',
 phone VARCHAR(32) NOT NULL DEFAULT '', service_area VARCHAR(500) NOT NULL DEFAULT '', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_store_tenant(tenant_id)
);
CREATE TABLE hm_worker (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, store_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL,
 phone VARCHAR(32) NOT NULL DEFAULT '', skills VARCHAR(1000) NOT NULL DEFAULT '', avatar VARCHAR(1000) NOT NULL DEFAULT '',
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_worker_tenant(tenant_id,store_id)
);
CREATE TABLE hm_service_category (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, name VARCHAR(100) NOT NULL, icon VARCHAR(1000) NOT NULL DEFAULT '',
 UNIQUE KEY uk_category_name(tenant_id,name)
);
CREATE TABLE hm_service (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, store_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL,
 category VARCHAR(100) NOT NULL DEFAULT '家政服务', description TEXT NOT NULL, price_cents INT NOT NULL, duration_minutes INT NOT NULL,
 cover VARCHAR(1000) NOT NULL DEFAULT '', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_service_tenant(tenant_id,store_id), CHECK(price_cents>=0), CHECK(duration_minutes>0)
);
CREATE TABLE hm_customer_address (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, contact_name VARCHAR(128) NOT NULL,
 phone VARCHAR(32) NOT NULL, address VARCHAR(500) NOT NULL, district_code VARCHAR(20) NOT NULL DEFAULT '', is_default BOOLEAN NOT NULL DEFAULT FALSE,
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_address_owner(tenant_id,customer_id)
);
CREATE TABLE hm_booking (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, service_id BIGINT NOT NULL,
 store_id BIGINT NOT NULL, worker_id BIGINT, starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'RESERVED', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_booking_tenant(tenant_id)
);
CREATE TABLE hm_worker_slot (
 tenant_id BIGINT NOT NULL, worker_id BIGINT NOT NULL, starts_at DATETIME NOT NULL, booking_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,worker_id,starts_at), KEY idx_slot_booking(tenant_id,booking_id)
);
CREATE TABLE hm_order (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, booking_id BIGINT NOT NULL,
 service_id BIGINT NOT NULL, service_name VARCHAR(128) NOT NULL, store_id BIGINT NOT NULL, worker_id BIGINT,
 price_cents INT NOT NULL, paid_cents INT NOT NULL DEFAULT 0, refunded_cents INT NOT NULL DEFAULT 0,
 status VARCHAR(30) NOT NULL DEFAULT 'UNPAID', contact_name VARCHAR(128) NOT NULL, phone VARCHAR(32) NOT NULL, address VARCHAR(500) NOT NULL,
 request_key VARCHAR(100) NOT NULL, pay_order_id BIGINT, customer_remark VARCHAR(500) NOT NULL DEFAULT '', version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, paid_at DATETIME, completed_at DATETIME,
 UNIQUE KEY uk_order_request(tenant_id,customer_id,request_key), UNIQUE KEY uk_order_booking(tenant_id,booking_id), KEY idx_order_tenant(tenant_id,status)
);
CREATE TABLE hm_order_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL, action VARCHAR(50) NOT NULL,
 actor_id BIGINT, detail VARCHAR(500) NOT NULL DEFAULT '', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_log_order(tenant_id,order_id)
);
CREATE TABLE hm_review (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 rating INT NOT NULL, content VARCHAR(2000) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_review(tenant_id,order_id), CHECK(rating BETWEEN 1 AND 5)
);
CREATE TABLE hm_aftersale (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 amount_cents INT NOT NULL, reason VARCHAR(1000) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', pay_refund_id BIGINT, previous_order_status VARCHAR(30),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY idx_aftersale_order(tenant_id,order_id), CHECK(amount_cents>0)
);
CREATE TABLE hm_settlement (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL, store_id BIGINT NOT NULL,
 gross_cents INT NOT NULL, refund_cents INT NOT NULL, net_cents INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_settlement_order(tenant_id,order_id), CHECK(net_cents>=0)
);
CREATE TABLE hm_notification_policy (
 tenant_id BIGINT NOT NULL, event_type VARCHAR(60) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 daily_limit INT NOT NULL DEFAULT 5, min_interval_minutes INT NOT NULL DEFAULT 30, quiet_start INT NOT NULL DEFAULT 22, quiet_end INT NOT NULL DEFAULT 8,
 allow_sms BOOLEAN NOT NULL DEFAULT FALSE, channels VARCHAR(100) NOT NULL DEFAULT 'MP,MINI',
 PRIMARY KEY(tenant_id,event_type), CHECK(daily_limit BETWEEN 0 AND 100), CHECK(min_interval_minutes BETWEEN 0 AND 1440),
 CHECK(quiet_start BETWEEN 0 AND 23), CHECK(quiet_end BETWEEN 0 AND 23)
);
-- Tenant 0 records platform caps. Tenant overrides can only tighten them.
INSERT INTO hm_notification_policy(tenant_id,event_type,daily_limit,allow_sms) VALUES(0,'*',5,FALSE);
CREATE TABLE hm_notification_preference (
 tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, event_type VARCHAR(60) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, allow_sms BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(tenant_id,customer_id,event_type)
);
CREATE TABLE hm_notification_outbox (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, order_id BIGINT,
 event_type VARCHAR(60) NOT NULL, level VARCHAR(10) NOT NULL, dedup_key VARCHAR(150) NOT NULL, payload TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0, next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 locked_until DATETIME, lock_token VARCHAR(100), sent_channel VARCHAR(10), last_error VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_notification_dedup(tenant_id,customer_id,dedup_key), KEY idx_notification_due(status,next_attempt_at)
);
CREATE TABLE hm_notification_delivery (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, outbox_id BIGINT NOT NULL,
 channel VARCHAR(10) NOT NULL, status VARCHAR(20) NOT NULL, provider_id VARCHAR(200), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_delivery(outbox_id,channel), KEY idx_delivery_limit(tenant_id,customer_id,created_at)
);
CREATE TABLE hm_notification_template (
 tenant_id BIGINT NOT NULL, event_type VARCHAR(60) NOT NULL, channel VARCHAR(10) NOT NULL, template_id VARCHAR(200) NOT NULL,
 field_mapping TEXT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY(tenant_id,event_type,channel)
);
CREATE TABLE hm_notification_consent (
 tenant_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, app_id VARCHAR(100) NOT NULL, template_id VARCHAR(200) NOT NULL,
 remaining INT NOT NULL DEFAULT 0, PRIMARY KEY(tenant_id,customer_id,app_id,template_id)
);
CREATE TABLE hm_service_sku (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, service_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL,
 price_cents INT NOT NULL, duration_minutes INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 KEY idx_sku_service(tenant_id,service_id), CHECK(price_cents>=0), CHECK(duration_minutes>0)
);
CREATE TABLE hm_service_extra (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, service_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL,
 price_cents INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', KEY idx_extra_service(tenant_id,service_id), CHECK(price_cents>=0)
);
CREATE TABLE hm_service_area (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, district_code VARCHAR(20) NOT NULL,
 extra_cents INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', KEY idx_area_tenant(tenant_id)
);
CREATE TABLE hm_service_area_relation (tenant_id BIGINT NOT NULL, service_id BIGINT NOT NULL, area_id BIGINT NOT NULL, PRIMARY KEY(tenant_id,service_id,area_id));
CREATE TABLE hm_service_booking_rule (
 tenant_id BIGINT NOT NULL, service_id BIGINT NOT NULL, min_advance_minutes INT NOT NULL DEFAULT 120, max_advance_days INT NOT NULL DEFAULT 90,
 allow_same_day BOOLEAN NOT NULL DEFAULT TRUE, time_slots_json TEXT, cancel_rule_json TEXT, PRIMARY KEY(tenant_id,service_id)
);
CREATE TABLE hm_order_item (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, order_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL,
 price_cents INT NOT NULL, quantity INT NOT NULL, total_cents INT NOT NULL, KEY idx_item_order(tenant_id,order_id)
);
CREATE TABLE hm_portal_content (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, content_type VARCHAR(32) NOT NULL, title VARCHAR(255) NOT NULL,
 summary VARCHAR(1000) NOT NULL DEFAULT '', content_html LONGTEXT, cover_image VARCHAR(1000) NOT NULL DEFAULT '',
 published BOOLEAN NOT NULL DEFAULT FALSE, KEY idx_content_type(tenant_id,content_type)
);
CREATE TABLE hm_home_content (tenant_id BIGINT NOT NULL, config_key VARCHAR(100) NOT NULL, config_value LONGTEXT, PRIMARY KEY(tenant_id,config_key));
CREATE TABLE hm_legacy_archive (source_table VARCHAR(100) NOT NULL, source_id VARCHAR(100) NOT NULL, tenant_id BIGINT NOT NULL, payload LONGTEXT NOT NULL, PRIMARY KEY(source_table,source_id,tenant_id));
