-- JD customer information imported from the daily RPA file.
-- Apply once before enabling file imports. Re-running the DDL is harmless.
-- Order numbers are identifiers, never numeric values.

CREATE TABLE IF NOT EXISTS jd_customer (
  customer_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '京东客户内部ID',
  display_nickname VARCHAR(160) NOT NULL DEFAULT '' COMMENT '最近一次看到的客户昵称，仅用于展示，不用于自动合并',
  contact_phone VARCHAR(32) NULL COMMENT '人工登记的客户手机号',
  contact_wechat VARCHAR(100) NULL COMMENT '人工登记的客户微信号',
  contact_updated_by BIGINT NULL COMMENT '最近登记联系方式的系统用户ID',
  contact_updated_at DATETIME NULL COMMENT '最近登记联系方式的时间',
  first_seen_at DATETIME NOT NULL COMMENT '首次在RPA记录中出现的时间',
  last_seen_at DATETIME NOT NULL COMMENT '最近在RPA记录中出现的时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '档案创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '档案更新时间',
  PRIMARY KEY (customer_id),
  KEY idx_jd_customer_nickname (display_nickname),
  KEY idx_jd_customer_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='京东客户档案；联系方式只保存在此表';

CREATE TABLE IF NOT EXISTS jd_customer_order (
  order_no VARCHAR(64) NOT NULL COMMENT '京东订单号，保留前导零',
  customer_id BIGINT NOT NULL COMMENT '所属京东客户内部ID；订单归属不自动变更',
  first_seen_at DATETIME NOT NULL COMMENT '订单首次出现在RPA记录中的时间',
  last_seen_at DATETIME NOT NULL COMMENT '订单最近出现在RPA记录中的时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
  PRIMARY KEY (order_no),
  KEY idx_jd_order_customer (customer_id,last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='京东订单与客户的唯一归属';

CREATE TABLE IF NOT EXISTS jd_rpa_import_batch (
  batch_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '导入批次ID',
  file_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '原始文件SHA-256，用于拒绝同一文件重复导入',
  file_name VARCHAR(255) NOT NULL COMMENT '上传时的文件名',
  source_date DATE NULL COMMENT 'RPA采集的业务日期；不限制同一天只能导入一个文件',
  imported_by_user_id BIGINT NOT NULL COMMENT '上传文件的系统用户ID',
  imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  source_row_count INT NOT NULL DEFAULT 0 COMMENT '文件中的有效记录行数',
  new_record_count INT NOT NULL DEFAULT 0 COMMENT '本次新增的服务记录数',
  duplicate_record_count INT NOT NULL DEFAULT 0 COMMENT '本次跳过的重复服务记录数',
  status VARCHAR(16) NOT NULL DEFAULT 'IMPORTING' COMMENT 'IMPORTING导入中；COMPLETED已完成；FAILED失败',
  error_message VARCHAR(500) NULL COMMENT '失败原因摘要',
  PRIMARY KEY (batch_id),
  UNIQUE KEY uk_jd_batch_file_sha256 (file_sha256),
  KEY idx_jd_batch_imported_at (imported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='京东客户RPA文件导入批次';

CREATE TABLE IF NOT EXISTS jd_service_record (
  record_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '客服接待记录ID',
  batch_id BIGINT NOT NULL COMMENT '首次导入此记录的批次ID',
  source_row_no INT NOT NULL COMMENT '原始文件中的行号，便于追查',
  dedup_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化时间、客服昵称、客户昵称和订单号集合的SHA-256',
  occurred_at DATETIME NOT NULL COMMENT 'RPA文件中的接待时间',
  service_nickname VARCHAR(100) NOT NULL COMMENT 'RPA文件中的客服昵称快照',
  customer_nickname VARCHAR(160) NOT NULL COMMENT 'RPA文件中的客户昵称快照',
  raw_order_nos VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '文件中的原始订单号文本，空值或逗号分隔均保留',
  customer_id BIGINT NULL COMMENT '订单解析出的客户ID；无订单或归属冲突时为空',
  match_status VARCHAR(16) NOT NULL COMMENT 'MATCHED已匹配；NO_ORDER无订单；CONFLICT订单归属冲突',
  imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次入库时间',
  PRIMARY KEY (record_id),
  UNIQUE KEY uk_jd_service_dedup (dedup_key),
  UNIQUE KEY uk_jd_service_batch_row (batch_id,source_row_no),
  KEY idx_jd_service_occurred (occurred_at,record_id),
  KEY idx_jd_service_customer (customer_id,occurred_at),
  KEY idx_jd_service_match (match_status,occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='京东客服RPA原始接待记录，一行文件对应一条';

CREATE TABLE IF NOT EXISTS jd_service_record_order (
  record_id BIGINT NOT NULL COMMENT '客服接待记录ID',
  order_no VARCHAR(64) NOT NULL COMMENT '拆分后的单个京东订单号',
  PRIMARY KEY (record_id,order_no),
  KEY idx_jd_record_order_no (order_no,record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服接待记录与订单的多对多关联';

-- All active roles receive the menu; the API uses the existing login requirement.
-- Apply after the tables above. New roles should also receive these two menu IDs.
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '京东客户信息',p.menu_id,10,'jd-customers','live/jd-customers/index',1,0,'C','0','0','','peoples','system',NOW()
FROM sys_menu p WHERE p.path='live-ops' AND p.menu_type='M' AND p.status='0'
AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE component='live/jd-customers/index'
  OR path='http://47.116.62.234/jd-customers-app/jd-customers.html');

INSERT INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
WHERE r.status='0' AND r.del_flag='0' AND m.status='0'
AND (m.component='live/jd-customers/index'
  OR m.path='http://47.116.62.234/jd-customers-app/jd-customers.html'
  OR (m.path='live-ops' AND m.menu_type='M'))
AND NOT EXISTS(SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
