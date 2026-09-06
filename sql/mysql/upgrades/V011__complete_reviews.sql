-- Apply once after V010. Existing reviews remain published and keep their original rating.
ALTER TABLE hm_review ADD COLUMN service_rating INT NOT NULL DEFAULT 5;
ALTER TABLE hm_review ADD COLUMN worker_rating INT NOT NULL DEFAULT 5;
ALTER TABLE hm_review ADD COLUMN tags_json VARCHAR(1000) NOT NULL DEFAULT '[]';
ALTER TABLE hm_review ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';
ALTER TABLE hm_review ADD COLUMN reply_content VARCHAR(1000) NULL;
ALTER TABLE hm_review ADD COLUMN replied_by BIGINT NULL;
ALTER TABLE hm_review ADD COLUMN replied_at DATETIME NULL;
ALTER TABLE hm_review ADD COLUMN recommended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE hm_review ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
UPDATE hm_review SET service_rating=rating,worker_rating=rating,status='PUBLISHED';
ALTER TABLE hm_review ADD CONSTRAINT chk_review_service_rating CHECK(service_rating BETWEEN 1 AND 5);
ALTER TABLE hm_review ADD CONSTRAINT chk_review_worker_rating CHECK(worker_rating BETWEEN 1 AND 5);
ALTER TABLE hm_review ADD CONSTRAINT chk_review_status CHECK(status IN ('DRAFT','PUBLISHED'));
CREATE INDEX idx_review_display ON hm_review(tenant_id,status,visible,recommended,id);
CREATE TABLE hm_review_image (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, review_id BIGINT NOT NULL,
 storage_key VARCHAR(200) NOT NULL, content_type VARCHAR(40) NOT NULL, request_key VARCHAR(100) NOT NULL,
 sort_order INT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_review_image_request(tenant_id,review_id,request_key), KEY idx_review_image(tenant_id,review_id,sort_order,id)
);
CREATE TABLE hm_review_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, review_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL, actor_id BIGINT NULL, actor_type VARCHAR(20) NOT NULL,
 detail TEXT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_review_log(tenant_id,review_id,id), KEY idx_review_order_log(tenant_id,order_id,id)
);
INSERT INTO hm_review_log(tenant_id,review_id,order_id,action,actor_type,detail,created_at)
 SELECT tenant_id,id,order_id,'MIGRATED','SYSTEM','升级前评价记录',created_at FROM hm_review;
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted) VALUES
(901140,'评价 · 回复','homemaking:reviews:reply',3,140,900001,'','','','',0,1,1,1,'migration','migration',FALSE);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater,deleted)
SELECT r.id,901140,r.tenant_id,'migration','migration',FALSE FROM system_role r
WHERE r.deleted=FALSE AND r.code IN ('super_admin','hm_platform','hm_owner','hm_manager','hm_support')
AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=r.id AND x.menu_id=901140 AND x.tenant_id=r.tenant_id AND x.deleted=FALSE);
INSERT INTO hm_schema_upgrade(version) VALUES('V011');
