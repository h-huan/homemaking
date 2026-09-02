-- Set @legacy_mini_app_id to the VERIFIED ORIGINAL miniapp AppID in the same connection.
-- Run only after hm-migrate-legacy.sql and registering that exact AppID for HQ in hm_wechat_app.
-- Do not guess an AppID from an OpenID. OAuth codes/session keys are never copied.
CREATE TEMPORARY TABLE hm_identity_assertion(ok INT NOT NULL CHECK(ok=1));
START TRANSACTION;
INSERT INTO hm_identity_assertion SELECT CASE WHEN COUNT(*)=1 THEN 1 ELSE 0 END FROM hm_wechat_app WHERE app_id=@legacy_mini_app_id AND tenant_id=1 AND kind='MINI';
INSERT INTO hm_wechat_identity(app_id,open_id,customer_id,union_id)
 SELECT @legacy_mini_app_id,auth_key,customer_id,NULLIF(unionid,'') FROM hm_legacy_snapshot.hm_user_auth WHERE auth_type='WECHAT_MINI' AND status='0' AND customer_id IS NOT NULL;
INSERT INTO hm_wechat_union(platform_id,union_id,customer_id)
 SELECT a.platform_id,i.union_id,i.customer_id FROM hm_wechat_identity i JOIN hm_wechat_app a ON a.app_id=i.app_id WHERE i.app_id=@legacy_mini_app_id AND i.union_id IS NOT NULL AND i.union_id<>'';
COMMIT;
DROP TEMPORARY TABLE hm_identity_assertion;
