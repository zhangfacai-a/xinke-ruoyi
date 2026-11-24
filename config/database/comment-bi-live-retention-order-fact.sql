-- Add Chinese column descriptions to the existing BI detail table.
-- Run on the xinkeerp database after verifying its columns match
-- add-bi-live-retention-order-fact.sql. No data rows are changed.
SET NAMES utf8mb4;

ALTER TABLE bi_live_retention_order_fact
  MODIFY COLUMN retention_id BIGINT NOT NULL COMMENT '挽单记录ID，对应live_retention_order.id；每条原订单一行',
  MODIFY COLUMN business_date DATE NOT NULL COMMENT '业务日期，取原订单创建日期',
  MODIFY COLUMN shop_id BIGINT NOT NULL COMMENT '原订单所属店铺ID',
  MODIFY COLUMN shop_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '原订单所属店铺名称，最近一次同步时的值',
  MODIFY COLUMN order_no VARCHAR(64) NOT NULL COMMENT '原订单号；按文本处理，避免长数字丢失精度',
  MODIFY COLUMN owner_id BIGINT NULL COMMENT '订单负责人用户ID；NULL表示未分配',
  MODIFY COLUMN owner_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '订单负责人姓名，最近一次同步时的值；未分配为空',
  MODIFY COLUMN progress VARCHAR(16) NOT NULL COMMENT '处理进度：PENDING待处理、FOLLOWING跟进中、COMPLETED已完成',
  MODIFY COLUMN retention_result VARCHAR(16) NOT NULL COMMENT '挽单结果：UNKNOWN未确定、SUCCESS成功、FAILED失败',
  MODIFY COLUMN retention_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '挽单销售额，单位元；非挽单成功时为0',
  MODIFY COLUMN referral_success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否转推成功：1是、0否',
  MODIFY COLUMN referral_shop_id BIGINT NULL COMMENT '转推店铺ID；未转推时为NULL',
  MODIFY COLUMN referral_shop_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '转推店铺名称，最近一次同步时的值；未转推为空',
  MODIFY COLUMN referral_order_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '转推订单号；未转推时为空',
  MODIFY COLUMN referral_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '转推金额，单位元；未转推时为0',
  MODIFY COLUMN freight_collected TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否征收运费：1是、0否；征收金额可为0',
  MODIFY COLUMN freight_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '征收运费金额，单位元；未征收时为0',
  MODIFY COLUMN first_followed_at DATETIME NULL COMMENT '首次客户跟进时间；尚未跟进时为NULL',
  MODIFY COLUMN completed_at DATETIME NULL COMMENT '订单完成时间；未完成时为NULL',
  MODIFY COLUMN completion_seconds BIGINT NULL COMMENT '完成耗时，完成时间减创建时间，单位秒；无法计算时为NULL',
  MODIFY COLUMN source_version INT NOT NULL DEFAULT 0 COMMENT '源订单版本号，用于识别修改',
  MODIFY COLUMN source_created_at DATETIME NOT NULL COMMENT '源订单创建时间',
  MODIFY COLUMN source_updated_at DATETIME NOT NULL COMMENT '源订单最后修改时间',
  MODIFY COLUMN synced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'BI明细最近同步时间',
  MODIFY COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '源订单是否已删除：1已删除、0有效；统计时只取0',
  COMMENT='BI挽单与转推订单明细表；一行一条原订单';

SHOW FULL COLUMNS FROM bi_live_retention_order_fact;
