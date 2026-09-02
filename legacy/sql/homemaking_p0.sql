-- P0 schema for homemaking business

create table if not exists hm_user_auth (
  auth_id bigint(20) not null auto_increment,
  user_id bigint(20) default null,
  customer_id bigint(20) default null,
  auth_type varchar(32) not null,
  auth_key varchar(128) not null,
  auth_secret varchar(255) default '',
  unionid varchar(64) default '',
  mobile varchar(20) default '',
  status char(1) not null default '0',
  create_time datetime default null,
  update_time datetime default null,
  primary key (auth_id),
  unique key uk_hm_auth_type_key (auth_type, auth_key),
  key idx_hm_auth_user_id (user_id),
  key idx_hm_auth_customer_id (customer_id)
) engine=innodb comment='homemaking auth binding';

create table if not exists hm_customer_user (
  customer_id bigint(20) not null auto_increment,
  user_id bigint(20) default null,
  mobile varchar(20) default '',
  nickname varchar(64) default '',
  avatar varchar(255) default '',
  gender char(1) default '0',
  real_name varchar(64) default '',
  status char(1) not null default '0',
  source_channel varchar(32) default 'WECHAT_MINI',
  org_id bigint(20) default null,
  last_login_ip varchar(128) default '',
  last_login_time datetime default null,
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (customer_id),
  key idx_hm_customer_mobile (mobile),
  key idx_hm_customer_org_id (org_id)
) engine=innodb comment='homemaking customer';

create table if not exists hm_customer_address (
  address_id bigint(20) not null auto_increment,
  customer_id bigint(20) not null,
  contact_name varchar(64) not null,
  contact_mobile varchar(20) not null,
  province_code varchar(20) default '',
  province_name varchar(64) default '',
  city_code varchar(20) default '',
  city_name varchar(64) default '',
  district_code varchar(20) default '',
  district_name varchar(64) default '',
  detail_address varchar(255) not null,
  longitude decimal(10,6) default null,
  latitude decimal(10,6) default null,
  is_default char(1) not null default '0',
  create_time datetime default null,
  update_time datetime default null,
  primary key (address_id),
  key idx_hm_address_customer_id (customer_id)
) engine=innodb comment='homemaking customer address';

create table if not exists hm_org (
  org_id bigint(20) not null auto_increment,
  parent_org_id bigint(20) not null default 0,
  org_name varchar(128) not null,
  org_type varchar(32) not null,
  contact_name varchar(64) default '',
  contact_mobile varchar(20) default '',
  province_code varchar(20) default '',
  city_code varchar(20) default '',
  district_code varchar(20) default '',
  status char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (org_id)
) engine=innodb comment='homemaking org';

create table if not exists hm_service_category (
  category_id bigint(20) not null auto_increment,
  parent_id bigint(20) not null default 0,
  category_name varchar(64) not null,
  category_icon varchar(255) default '',
  banner_image varchar(255) default '',
  banner_image_storage varchar(32) default 'local',
  banner_image_object_key varchar(255) default '',
  sort_no int(11) not null default 0,
  show_in_miniapp char(1) not null default '1',
  show_in_portal char(1) not null default '1',
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (category_id)
) engine=innodb comment='homemaking service category';

create table if not exists hm_service_item (
  service_item_id bigint(20) not null auto_increment,
  category_id bigint(20) not null,
  service_name varchar(128) not null,
  org_id bigint(20) default null,
  service_sub_title varchar(255) default '',
  service_cover varchar(255) default '',
  service_cover_storage varchar(32) default 'local',
  service_cover_object_key varchar(255) default '',
  service_desc varchar(500) default '',
  service_content text,
  service_duration int(11) default 0,
  sale_type varchar(20) not null default 'fixed',
  base_price decimal(10,2) not null default 0.00,
  unit_name varchar(32) default '',
  need_manual_confirm char(1) not null default '0',
  allow_assign_worker char(1) not null default '0',
  show_in_miniapp char(1) not null default '1',
  show_in_portal char(1) not null default '1',
  status char(1) not null default '0',
  del_flag char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (service_item_id),
  key idx_hm_service_category_id (category_id),
  key idx_hm_service_status (status)
) engine=innodb comment='homemaking service item';

create table if not exists hm_service_sku (
  sku_id bigint(20) not null auto_increment,
  service_item_id bigint(20) not null,
  sku_name varchar(128) not null,
  price decimal(10,2) not null default 0.00,
  original_price decimal(10,2) default 0.00,
  duration_minute int(11) default 0,
  sort_no int(11) not null default 0,
  status char(1) not null default '0',
  primary key (sku_id),
  key idx_hm_sku_service_item_id (service_item_id)
) engine=innodb comment='homemaking service sku';

create table if not exists hm_service_extra_item (
  extra_item_id bigint(20) not null auto_increment,
  service_item_id bigint(20) not null,
  extra_name varchar(128) not null,
  extra_price decimal(10,2) not null default 0.00,
  charge_type varchar(20) not null default 'once',
  sort_no int(11) not null default 0,
  status char(1) not null default '0',
  primary key (extra_item_id),
  key idx_hm_extra_service_item_id (service_item_id)
) engine=innodb comment='homemaking service extra item';

create table if not exists hm_service_area (
  area_id bigint(20) not null auto_increment,
  province_code varchar(20) default '',
  province_name varchar(64) default '',
  city_code varchar(20) default '',
  city_name varchar(64) default '',
  district_code varchar(20) default '',
  district_name varchar(64) default '',
  area_name varchar(128) not null,
  org_id bigint(20) default null,
  extra_fee decimal(10,2) not null default 0.00,
  status char(1) not null default '0',
  primary key (area_id)
) engine=innodb comment='homemaking service area';

create table if not exists hm_service_item_area_rel (
  id bigint(20) not null auto_increment,
  service_item_id bigint(20) not null,
  area_id bigint(20) not null,
  primary key (id),
  key idx_hm_item_area_service_item_id (service_item_id),
  key idx_hm_item_area_area_id (area_id)
) engine=innodb comment='homemaking service item area relation';

create table if not exists hm_booking_rule (
  rule_id bigint(20) not null auto_increment,
  service_item_id bigint(20) not null,
  advance_days int(11) not null default 0,
  min_advance_minutes int(11) not null default 0,
  max_advance_days int(11) not null default 30,
  allow_same_day char(1) not null default '1',
  time_slots_json text,
  cancel_rule_json text,
  status char(1) not null default '0',
  primary key (rule_id),
  unique key uk_hm_booking_rule_service_item_id (service_item_id)
) engine=innodb comment='homemaking booking rule';

create table if not exists hm_worker_profile (
  worker_profile_id bigint(20) not null auto_increment,
  user_id bigint(20) not null,
  id_card_no varchar(32) default '',
  worker_type varchar(32) default '',
  intro varchar(1000) default '',
  service_star decimal(3,1) default 5.0,
  service_count int(11) default 0,
  employment_status char(1) not null default '0',
  work_status char(1) not null default '0',
  org_id bigint(20) default null,
  status char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (worker_profile_id),
  unique key uk_hm_worker_user_id (user_id)
) engine=innodb comment='homemaking worker profile';

create table if not exists hm_worker_service_rel (
  id bigint(20) not null auto_increment,
  worker_id bigint(20) not null,
  service_item_id bigint(20) not null,
  primary key (id),
  key idx_hm_worker_service_worker_id (worker_id)
) engine=innodb comment='homemaking worker service relation';

create table if not exists hm_worker_area_rel (
  id bigint(20) not null auto_increment,
  worker_id bigint(20) not null,
  area_id bigint(20) not null,
  primary key (id),
  key idx_hm_worker_area_worker_id (worker_id)
) engine=innodb comment='homemaking worker area relation';

create table if not exists hm_worker_schedule (
  schedule_id bigint(20) not null auto_increment,
  worker_id bigint(20) not null,
  work_date date not null,
  start_time time not null,
  end_time time not null,
  schedule_status char(1) not null default '0',
  primary key (schedule_id),
  key idx_hm_worker_schedule_worker_date (worker_id, work_date)
) engine=innodb comment='homemaking worker schedule';

create table if not exists hm_order (
  order_id bigint(20) not null auto_increment,
  order_no varchar(64) not null,
  customer_id bigint(20) not null,
  org_id bigint(20) default null,
  service_item_id bigint(20) not null,
  sku_id bigint(20) default null,
  order_type varchar(20) not null default 'service',
  order_status varchar(2) not null default '10',
  pay_status char(1) not null default '0',
  assign_status char(1) not null default '0',
  contact_name varchar(64) not null,
  contact_mobile varchar(20) not null,
  service_address varchar(255) not null,
  province_code varchar(20) default '',
  city_code varchar(20) default '',
  district_code varchar(20) default '',
  appointment_date date not null,
  appointment_time_slot varchar(64) not null,
  service_start_time datetime default null,
  service_end_time datetime default null,
  base_amount decimal(10,2) not null default 0.00,
  extra_amount decimal(10,2) not null default 0.00,
  discount_amount decimal(10,2) not null default 0.00,
  pay_amount decimal(10,2) not null default 0.00,
  pay_time datetime default null,
  pay_channel varchar(32) default '',
  source_channel varchar(32) default 'WECHAT_MINI',
  customer_remark varchar(500) default '',
  cancel_reason varchar(255) default '',
  create_time datetime default null,
  update_time datetime default null,
  primary key (order_id),
  unique key uk_hm_order_no (order_no),
  key idx_hm_order_customer_id (customer_id),
  key idx_hm_order_status (order_status)
) engine=innodb comment='homemaking order';

create table if not exists hm_order_item (
  order_item_id bigint(20) not null auto_increment,
  order_id bigint(20) not null,
  service_item_id bigint(20) not null,
  sku_id bigint(20) default null,
  item_name varchar(128) not null,
  item_price decimal(10,2) not null default 0.00,
  quantity int(11) not null default 1,
  item_amount decimal(10,2) not null default 0.00,
  primary key (order_item_id),
  key idx_hm_order_item_order_id (order_id)
) engine=innodb comment='homemaking order item';

create table if not exists hm_order_assign (
  assign_id bigint(20) not null auto_increment,
  order_id bigint(20) not null,
  worker_id bigint(20) not null,
  assign_status char(1) not null default '1',
  assign_time datetime default null,
  accept_time datetime default null,
  finish_time datetime default null,
  cancel_time datetime default null,
  remark varchar(500) default '',
  primary key (assign_id),
  key idx_hm_order_assign_order_id (order_id)
) engine=innodb comment='homemaking order assign';

create table if not exists hm_order_operate_log (
  log_id bigint(20) not null auto_increment,
  order_id bigint(20) not null,
  operator_type varchar(20) not null,
  operator_id bigint(20) default null,
  operator_name varchar(64) default '',
  action_type varchar(32) not null,
  action_desc varchar(500) default '',
  create_time datetime default null,
  primary key (log_id),
  key idx_hm_order_log_order_id (order_id)
) engine=innodb comment='homemaking order operate log';

create table if not exists hm_order_refund (
  refund_id bigint(20) not null auto_increment,
  order_id bigint(20) not null,
  refund_no varchar(64) not null,
  refund_status char(1) not null default '0',
  refund_amount decimal(10,2) not null default 0.00,
  refund_reason varchar(255) default '',
  apply_time datetime default null,
  audit_time datetime default null,
  finish_time datetime default null,
  primary key (refund_id),
  unique key uk_hm_refund_no (refund_no),
  key idx_hm_refund_order_id (order_id)
) engine=innodb comment='homemaking order refund';

create table if not exists hm_review (
  review_id bigint(20) not null auto_increment,
  order_id bigint(20) not null,
  customer_id bigint(20) not null,
  worker_id bigint(20) default null,
  score int(11) not null,
  tags_json varchar(1000) default '',
  content varchar(2000) default '',
  images_json text,
  is_anonymous char(1) not null default '0',
  status char(1) not null default '0',
  create_time datetime default null,
  primary key (review_id),
  key idx_hm_review_order_id (order_id)
) engine=innodb comment='homemaking review';

create table if not exists hm_portal_content (
  content_id bigint(20) not null auto_increment,
  content_type varchar(32) not null,
  title varchar(255) not null,
  sub_title varchar(255) default '',
  cover_image varchar(255) default '',
  cover_image_storage varchar(32) default 'local',
  cover_image_object_key varchar(255) default '',
  summary varchar(1000) default '',
  content_html longtext,
  sort_no int(11) not null default 0,
  publish_status char(1) not null default '0',
  publish_time datetime default null,
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  primary key (content_id),
  key idx_hm_portal_content_type (content_type),
  key idx_hm_portal_publish_status (publish_status)
) engine=innodb comment='homemaking portal content';

create table if not exists hm_mini_nav_config (
  nav_id bigint(20) not null auto_increment,
  org_id bigint(20) default null,
  nav_name varchar(64) not null,
  nav_icon varchar(255) default '',
  nav_path varchar(255) not null,
  sort_no int(11) not null default 0,
  status char(1) not null default '0',
  primary key (nav_id)
) engine=innodb comment='homemaking mini nav config';

create table if not exists hm_mini_home_config (
  config_id bigint(20) not null auto_increment,
  org_id bigint(20) default null,
  config_key varchar(64) not null,
  config_value longtext,
  status char(1) not null default '0',
  primary key (config_id),
  unique key uk_hm_home_config_key (config_key)
) engine=innodb comment='homemaking mini home config';

alter table hm_service_category add column if not exists banner_image_storage varchar(32) default 'local' after banner_image;
alter table hm_service_category add column if not exists banner_image_object_key varchar(255) default '' after banner_image_storage;
alter table hm_service_item add column if not exists service_cover_storage varchar(32) default 'local' after service_cover;
alter table hm_service_item add column if not exists service_cover_object_key varchar(255) default '' after service_cover_storage;
alter table hm_portal_content add column if not exists cover_image_storage varchar(32) default 'local' after cover_image;
alter table hm_portal_content add column if not exists cover_image_object_key varchar(255) default '' after cover_image_storage;

create table if not exists hm_lead (
  lead_id bigint(20) not null auto_increment,
  lead_type varchar(32) not null,
  name varchar(64) not null,
  mobile varchar(20) not null,
  city_name varchar(64) default '',
  org_id bigint(20) default null,
  intent_service varchar(128) default '',
  content varchar(1000) default '',
  source_channel varchar(32) default '',
  follow_status char(1) not null default '0',
  follow_by varchar(64) default '',
  follow_time datetime default null,
  create_time datetime default null,
  update_time datetime default null,
  primary key (lead_id),
  key idx_hm_lead_mobile (mobile),
  key idx_hm_lead_follow_status (follow_status)
) engine=innodb comment='homemaking portal lead';

insert into hm_service_category(category_id, parent_id, category_name, category_icon, sort_no, show_in_miniapp, show_in_portal, status, del_flag, create_by, create_time)
select 1, 0, '日常保洁', 'clean', 1, '1', '1', '0', '0', 'admin', sysdate()
where not exists (select 1 from hm_service_category where category_id = 1);

insert into hm_service_category(category_id, parent_id, category_name, category_icon, sort_no, show_in_miniapp, show_in_portal, status, del_flag, create_by, create_time)
select 2, 0, '深度清洁', 'sparkles', 2, '1', '1', '0', '0', 'admin', sysdate()
where not exists (select 1 from hm_service_category where category_id = 2);

insert into hm_service_area(area_id, province_name, city_name, district_name, area_name, extra_fee, status)
select 1, '上海市', '上海市', '浦东新区', '浦东新区', 0.00, '0'
where not exists (select 1 from hm_service_area where area_id = 1);

insert into hm_service_area(area_id, province_name, city_name, district_name, area_name, extra_fee, status)
select 2, '上海市', '上海市', '闵行区', '闵行区', 10.00, '0'
where not exists (select 1 from hm_service_area where area_id = 2);

insert into hm_service_item(service_item_id, category_id, service_name, service_sub_title, service_cover, service_desc, service_content, service_duration, sale_type, base_price, unit_name, need_manual_confirm, allow_assign_worker, show_in_miniapp, show_in_portal, status, del_flag, create_by, create_time)
select 1, 1, '2小时日常保洁', '适合日常家庭清洁', '', '标准家庭保洁服务', '<p>标准家庭保洁服务</p>', 120, 'fixed', 129.00, '次', '0', '1', '1', '1', '0', '0', 'admin', sysdate()
where not exists (select 1 from hm_service_item where service_item_id = 1);

insert into hm_service_sku(sku_id, service_item_id, sku_name, price, original_price, duration_minute, sort_no, status)
select 1, 1, '2小时标准保洁', 129.00, 159.00, 120, 1, '0'
where not exists (select 1 from hm_service_sku where sku_id = 1);

insert into hm_service_extra_item(extra_item_id, service_item_id, extra_name, extra_price, charge_type, sort_no, status)
select 1, 1, '厨房深度清洁', 39.00, 'once', 1, '0'
where not exists (select 1 from hm_service_extra_item where extra_item_id = 1);

insert into hm_service_extra_item(extra_item_id, service_item_id, extra_name, extra_price, charge_type, sort_no, status)
select 2, 1, '冰箱清洁', 29.00, 'once', 2, '0'
where not exists (select 1 from hm_service_extra_item where extra_item_id = 2);

insert into hm_service_item_area_rel(id, service_item_id, area_id)
select 1, 1, 1
where not exists (select 1 from hm_service_item_area_rel where id = 1);

insert into hm_service_item_area_rel(id, service_item_id, area_id)
select 2, 1, 2
where not exists (select 1 from hm_service_item_area_rel where id = 2);

insert into hm_booking_rule(rule_id, service_item_id, advance_days, min_advance_minutes, max_advance_days, allow_same_day, time_slots_json, cancel_rule_json, status)
select 1, 1, 0, 120, 7, '1', '["09:00-11:00","13:00-15:00","16:00-18:00"]', '{"freeCancelMinutes":60}', '0'
where not exists (select 1 from hm_booking_rule where rule_id = 1);

insert into hm_mini_nav_config(nav_id, org_id, nav_name, nav_icon, nav_path, sort_no, status)
select 1, null, '首页', 'home', '/pages/home/index', 1, '0'
where not exists (select 1 from hm_mini_nav_config where nav_id = 1);

insert into hm_mini_nav_config(nav_id, org_id, nav_name, nav_icon, nav_path, sort_no, status)
select 2, null, '订单', 'order', '/pages/order/list/index', 2, '0'
where not exists (select 1 from hm_mini_nav_config where nav_id = 2);

insert into hm_mini_nav_config(nav_id, org_id, nav_name, nav_icon, nav_path, sort_no, status)
select 3, null, '地址', 'location', '/pages/address/list/index', 3, '0'
where not exists (select 1 from hm_mini_nav_config where nav_id = 3);

insert into hm_mini_home_config(config_id, org_id, config_key, config_value, status)
select 1, null, 'bannerList', '[{"imageUrl":"","title":"春季焕新保洁","linkType":"service","linkValue":"1"}]', '0'
where not exists (select 1 from hm_mini_home_config where config_id = 1);

insert into hm_mini_home_config(config_id, org_id, config_key, config_value, status)
select 2, null, 'advantageList', '["实名认证","标准服务","售后保障"]', '0'
where not exists (select 1 from hm_mini_home_config where config_id = 2);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3000, '家政业务', 0, 5, 'hm', null, '', 'Homemaking', 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '', null, '家政业务目录'
where not exists (select 1 from sys_menu where menu_id = 3000);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3001, '服务类目', 3000, 1, 'category', 'hm/category/index', '', 'HmCategory', 1, 0, 'C', '0', '0', 'hm:category:list', 'tree', 'admin', sysdate(), '', null, '服务类目'
where not exists (select 1 from sys_menu where menu_id = 3001);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3002, '服务商品', 3000, 2, 'service', 'hm/service/index', '', 'HmService', 1, 0, 'C', '0', '0', 'hm:service:list', 'form', 'admin', sysdate(), '', null, '服务商品'
where not exists (select 1 from sys_menu where menu_id = 3002);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3003, '服务区域', 3000, 3, 'area', 'hm/area/index', '', 'HmArea', 1, 0, 'C', '0', '0', 'hm:area:list', 'map', 'admin', sysdate(), '', null, '服务区域'
where not exists (select 1 from sys_menu where menu_id = 3003);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3004, '服务人员', 3000, 4, 'worker', 'hm/worker/index', '', 'HmWorker', 1, 0, 'C', '0', '0', 'hm:worker:list', 'peoples', 'admin', sysdate(), '', null, '服务人员'
where not exists (select 1 from sys_menu where menu_id = 3004);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3005, '订单管理', 3000, 5, 'order', 'hm/order/index', '', 'HmOrder', 1, 0, 'C', '0', '0', 'hm:order:list', 'order', 'admin', sysdate(), '', null, '订单管理'
where not exists (select 1 from sys_menu where menu_id = 3005);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3006, '小程序运营', 3000, 6, 'mini-config', 'hm/miniConfig/index', '', 'HmMiniConfig', 1, 0, 'C', '0', '0', 'hm:miniConfig:list', 'build', 'admin', sysdate(), '', null, '小程序运营'
where not exists (select 1 from sys_menu where menu_id = 3006);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3101, '类目查询', 3001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:category:list', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3101);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3102, '类目新增', 3001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:category:add', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3102);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3103, '类目修改', 3001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:category:edit', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3103);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3201, '服务查询', 3002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:service:list', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3201);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3202, '服务新增', 3002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:service:add', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3202);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3203, '服务修改', 3002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:service:edit', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3203);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3301, '区域查询', 3003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:area:list', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3301);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3302, '区域新增', 3003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:area:add', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3302);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3303, '区域修改', 3003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:area:edit', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3303);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3401, '人员查询', 3004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:worker:list', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3401);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3402, '人员新增', 3004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:worker:add', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3402);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3403, '人员修改', 3004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:worker:edit', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3403);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3501, '订单查询', 3005, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:list', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3501);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3502, '订单详情', 3005, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:detail', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3502);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3503, '订单派单', 3005, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:assign', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3503);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3504, '订单取消', 3005, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:cancel', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3504);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3601, '配置查询', 3006, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:miniConfig:list', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3601);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3602, '配置修改', 3006, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:miniConfig:edit', '#', 'admin', sysdate(), '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3602);
