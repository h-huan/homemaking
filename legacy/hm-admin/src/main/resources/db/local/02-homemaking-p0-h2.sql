-- P0 schema for homemaking business

create table if not exists hm_user_auth (
  auth_id bigint not null auto_increment,
  user_id bigint default null,
  customer_id bigint default null,
  auth_type varchar(32) not null,
  auth_key varchar(128) not null,
  auth_secret varchar(255) default '',
  unionid varchar(64) default '',
  mobile varchar(20) default '',
  status char(1) not null default '0',
  create_time datetime default null,
  update_time datetime default null,
  primary key (auth_id), unique (auth_type, auth_key)
);

create table if not exists hm_customer_user (
  customer_id bigint not null auto_increment,
  user_id bigint default null,
  mobile varchar(20) default '',
  nickname varchar(64) default '',
  avatar varchar(255) default '',
  gender char(1) default '0',
  real_name varchar(64) default '',
  status char(1) not null default '0',
  source_channel varchar(32) default 'WECHAT_MINI',
  org_id bigint default null,
  last_login_ip varchar(128) default '',
  last_login_time datetime default null,
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (customer_id)
);

create table if not exists hm_customer_address (
  address_id bigint not null auto_increment,
  customer_id bigint not null,
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
  primary key (address_id)
);

create table if not exists hm_org (
  org_id bigint not null auto_increment,
  parent_org_id bigint not null default 0,
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
);

create table if not exists hm_service_category (
  category_id bigint not null auto_increment,
  parent_id bigint not null default 0,
  category_name varchar(64) not null,
  category_icon varchar(255) default '',
  banner_image varchar(255) default '',
  sort_no int not null default 0,
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
);

create table if not exists hm_service_item (
  service_item_id bigint not null auto_increment,
  category_id bigint not null,
  service_name varchar(128) not null,
  org_id bigint default null,
  service_sub_title varchar(255) default '',
  service_cover varchar(255) default '',
  service_desc varchar(500) default '',
  service_content text,
  service_duration int default 0,
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
  primary key (service_item_id)
);

create table if not exists hm_service_sku (
  sku_id bigint not null auto_increment,
  service_item_id bigint not null,
  sku_name varchar(128) not null,
  price decimal(10,2) not null default 0.00,
  original_price decimal(10,2) default 0.00,
  duration_minute int default 0,
  sort_no int not null default 0,
  status char(1) not null default '0',
  primary key (sku_id)
);

create table if not exists hm_service_extra_item (
  extra_item_id bigint not null auto_increment,
  service_item_id bigint not null,
  extra_name varchar(128) not null,
  extra_price decimal(10,2) not null default 0.00,
  charge_type varchar(20) not null default 'once',
  sort_no int not null default 0,
  status char(1) not null default '0',
  primary key (extra_item_id)
);

create table if not exists hm_service_area (
  area_id bigint not null auto_increment,
  province_code varchar(20) default '',
  province_name varchar(64) default '',
  city_code varchar(20) default '',
  city_name varchar(64) default '',
  district_code varchar(20) default '',
  district_name varchar(64) default '',
  area_name varchar(128) not null,
  org_id bigint default null,
  extra_fee decimal(10,2) not null default 0.00,
  status char(1) not null default '0',
  primary key (area_id)
);

create table if not exists hm_service_item_area_rel (
  id bigint not null auto_increment,
  service_item_id bigint not null,
  area_id bigint not null,
  primary key (id)
);

create table if not exists hm_booking_rule (
  rule_id bigint not null auto_increment,
  service_item_id bigint not null,
  advance_days int not null default 0,
  min_advance_minutes int not null default 0,
  max_advance_days int not null default 30,
  allow_same_day char(1) not null default '1',
  time_slots_json text,
  cancel_rule_json text,
  status char(1) not null default '0',
  primary key (rule_id), unique (service_item_id)
);

create table if not exists hm_worker_profile (
  worker_profile_id bigint not null auto_increment,
  user_id bigint not null,
  id_card_no varchar(32) default '',
  worker_type varchar(32) default '',
  intro varchar(1000) default '',
  service_star decimal(3,1) default 5.0,
  service_count int default 0,
  employment_status char(1) not null default '0',
  work_status char(1) not null default '0',
  org_id bigint default null,
  status char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  remark varchar(500) default null,
  primary key (worker_profile_id), unique (user_id)
);

create table if not exists hm_worker_service_rel (
  id bigint not null auto_increment,
  worker_id bigint not null,
  service_item_id bigint not null,
  primary key (id)
);

create table if not exists hm_worker_area_rel (
  id bigint not null auto_increment,
  worker_id bigint not null,
  area_id bigint not null,
  primary key (id)
);

create table if not exists hm_worker_schedule (
  schedule_id bigint not null auto_increment,
  worker_id bigint not null,
  work_date date not null,
  start_time time not null,
  end_time time not null,
  schedule_status char(1) not null default '0',
  primary key (schedule_id)
);

create table if not exists hm_order (
  order_id bigint not null auto_increment,
  order_no varchar(64) not null,
  customer_id bigint not null,
  org_id bigint default null,
  service_item_id bigint not null,
  sku_id bigint default null,
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
  primary key (order_id), unique (order_no)
);

create table if not exists hm_order_item (
  order_item_id bigint not null auto_increment,
  order_id bigint not null,
  service_item_id bigint not null,
  sku_id bigint default null,
  item_name varchar(128) not null,
  item_price decimal(10,2) not null default 0.00,
  quantity int not null default 1,
  item_amount decimal(10,2) not null default 0.00,
  primary key (order_item_id)
);

create table if not exists hm_order_assign (
  assign_id bigint not null auto_increment,
  order_id bigint not null,
  worker_id bigint not null,
  assign_status char(1) not null default '1',
  assign_time datetime default null,
  accept_time datetime default null,
  finish_time datetime default null,
  cancel_time datetime default null,
  remark varchar(500) default '',
  primary key (assign_id)
);

create table if not exists hm_order_operate_log (
  log_id bigint not null auto_increment,
  order_id bigint not null,
  operator_type varchar(20) not null,
  operator_id bigint default null,
  operator_name varchar(64) default '',
  action_type varchar(32) not null,
  action_desc varchar(500) default '',
  create_time datetime default null,
  primary key (log_id)
);

create table if not exists hm_order_refund (
  refund_id bigint not null auto_increment,
  order_id bigint not null,
  refund_no varchar(64) not null,
  refund_status char(1) not null default '0',
  refund_amount decimal(10,2) not null default 0.00,
  refund_reason varchar(255) default '',
  apply_time datetime default null,
  audit_time datetime default null,
  finish_time datetime default null,
  primary key (refund_id),
  unique (refund_no)
);

create table if not exists hm_review (
  review_id bigint not null auto_increment,
  order_id bigint not null,
  customer_id bigint not null,
  worker_id bigint default null,
  score int not null,
  tags_json varchar(1000) default '',
  content varchar(2000) default '',
  images_json text,
  is_anonymous char(1) not null default '0',
  status char(1) not null default '0',
  create_time datetime default null,
  primary key (review_id)
);

create table if not exists hm_portal_content (
  content_id bigint not null auto_increment,
  content_type varchar(32) not null,
  title varchar(255) not null,
  sub_title varchar(255) default '',
  cover_image varchar(255) default '',
  summary varchar(1000) default '',
  content_html longtext,
  sort_no int not null default 0,
  publish_status char(1) not null default '0',
  publish_time datetime default null,
  create_by varchar(64) default '',
  create_time datetime default null,
  update_by varchar(64) default '',
  update_time datetime default null,
  primary key (content_id)
);

create table if not exists hm_mini_nav_config (
  nav_id bigint not null auto_increment,
  org_id bigint default null,
  nav_name varchar(64) not null,
  nav_icon varchar(255) default '',
  nav_path varchar(255) not null,
  sort_no int not null default 0,
  status char(1) not null default '0',
  primary key (nav_id)
);

create table if not exists hm_mini_home_config (
  config_id bigint not null auto_increment,
  org_id bigint default null,
  config_key varchar(64) not null,
  config_value longtext,
  status char(1) not null default '0',
  primary key (config_id), unique (config_key)
);

create table if not exists hm_lead (
  lead_id bigint not null auto_increment,
  lead_type varchar(32) not null,
  name varchar(64) not null,
  mobile varchar(20) not null,
  city_name varchar(64) default '',
  org_id bigint default null,
  intent_service varchar(128) default '',
  content varchar(1000) default '',
  source_channel varchar(32) default '',
  follow_status char(1) not null default '0',
  follow_by varchar(64) default '',
  follow_time datetime default null,
  create_time datetime default null,
  update_time datetime default null,
  primary key (lead_id)
);

insert into hm_service_category(category_id, parent_id, category_name, category_icon, sort_no, show_in_miniapp, show_in_portal, status, del_flag, create_by, create_time)
select 1, 0, '日常保洁', 'clean', 1, '1', '1', '0', '0', 'admin', CURRENT_TIMESTAMP
where not exists (select 1 from hm_service_category where category_id = 1);

insert into hm_service_category(category_id, parent_id, category_name, category_icon, sort_no, show_in_miniapp, show_in_portal, status, del_flag, create_by, create_time)
select 2, 0, '深度清洁', 'sparkles', 2, '1', '1', '0', '0', 'admin', CURRENT_TIMESTAMP
where not exists (select 1 from hm_service_category where category_id = 2);

insert into hm_service_area(area_id, province_name, city_name, district_name, area_name, extra_fee, status)
select 1, '上海市', '上海市', '浦东新区', '浦东新区', 0.00, '0'
where not exists (select 1 from hm_service_area where area_id = 1);

insert into hm_service_area(area_id, province_name, city_name, district_name, area_name, extra_fee, status)
select 2, '上海市', '上海市', '闵行区', '闵行区', 10.00, '0'
where not exists (select 1 from hm_service_area where area_id = 2);

insert into hm_service_item(service_item_id, category_id, service_name, service_sub_title, service_cover, service_desc, service_content, service_duration, sale_type, base_price, unit_name, need_manual_confirm, allow_assign_worker, show_in_miniapp, show_in_portal, status, del_flag, create_by, create_time)
select 1, 1, '2小时日常保洁', '适合日常家庭清洁', '', '标准家庭保洁服务', '<p>标准家庭保洁服务</p>', 120, 'fixed', 129.00, '次', '0', '1', '1', '1', '0', '0', 'admin', CURRENT_TIMESTAMP
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
select 3000, '家政业务', 0, 5, 'hm', null, '', 'Homemaking', 1, 0, 'M', '0', '0', '', 'education', 'admin', CURRENT_TIMESTAMP, '', null, '家政业务目录'
where not exists (select 1 from sys_menu where menu_id = 3000);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3001, '服务类目', 3000, 1, 'category', 'hm/category/index', '', 'HmCategory', 1, 0, 'C', '0', '0', 'hm:category:list', 'tree', 'admin', CURRENT_TIMESTAMP, '', null, '服务类目'
where not exists (select 1 from sys_menu where menu_id = 3001);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3002, '服务商品', 3000, 2, 'service', 'hm/service/index', '', 'HmService', 1, 0, 'C', '0', '0', 'hm:service:list', 'form', 'admin', CURRENT_TIMESTAMP, '', null, '服务商品'
where not exists (select 1 from sys_menu where menu_id = 3002);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3003, '服务区域', 3000, 3, 'area', 'hm/area/index', '', 'HmArea', 1, 0, 'C', '0', '0', 'hm:area:list', 'map', 'admin', CURRENT_TIMESTAMP, '', null, '服务区域'
where not exists (select 1 from sys_menu where menu_id = 3003);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3004, '服务人员', 3000, 4, 'worker', 'hm/worker/index', '', 'HmWorker', 1, 0, 'C', '0', '0', 'hm:worker:list', 'peoples', 'admin', CURRENT_TIMESTAMP, '', null, '服务人员'
where not exists (select 1 from sys_menu where menu_id = 3004);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3005, '订单管理', 3000, 5, 'order', 'hm/order/index', '', 'HmOrder', 1, 0, 'C', '0', '0', 'hm:order:list', 'order', 'admin', CURRENT_TIMESTAMP, '', null, '订单管理'
where not exists (select 1 from sys_menu where menu_id = 3005);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3006, '小程序运营', 3000, 6, 'mini-config', 'hm/miniConfig/index', '', 'HmMiniConfig', 1, 0, 'C', '0', '0', 'hm:miniConfig:list', 'build', 'admin', CURRENT_TIMESTAMP, '', null, '小程序运营'
where not exists (select 1 from sys_menu where menu_id = 3006);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3101, '类目查询', 3001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:category:list', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3101);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3102, '类目新增', 3001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:category:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3102);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3103, '类目修改', 3001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:category:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3103);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3201, '服务查询', 3002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:service:list', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3201);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3202, '服务新增', 3002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:service:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3202);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3203, '服务修改', 3002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:service:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3203);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3301, '区域查询', 3003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:area:list', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3301);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3302, '区域新增', 3003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:area:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3302);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3303, '区域修改', 3003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:area:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3303);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3401, '人员查询', 3004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:worker:list', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3401);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3402, '人员新增', 3004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:worker:add', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3402);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3403, '人员修改', 3004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:worker:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3403);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3501, '订单查询', 3005, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:list', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3501);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3502, '订单详情', 3005, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:detail', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3502);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3503, '订单派单', 3005, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:assign', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3503);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3504, '订单取消', 3005, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:order:cancel', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3504);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3601, '配置查询', 3006, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:miniConfig:list', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3601);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
select 3602, '配置修改', 3006, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'hm:miniConfig:edit', '#', 'admin', CURRENT_TIMESTAMP, '', null, ''
where not exists (select 1 from sys_menu where menu_id = 3602);

