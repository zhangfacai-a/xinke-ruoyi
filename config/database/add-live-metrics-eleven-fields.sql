-- 直播大屏 11 项原始指标。累计快照保存在 submission，班次只对可累计值计算差额。
ALTER TABLE live_metric_submission
  ADD COLUMN gmv_total DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '直播间累计成交金额' AFTER interaction_total,
  ADD COLUMN ad_cost_total DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '千川累计消耗' AFTER gmv_total,
  ADD COLUMN exposure_watch_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '曝光-观看率' AFTER ad_cost_total,
  ADD COLUMN watch_follow_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '观看-关注率' AFTER exposure_watch_rate,
  ADD COLUMN watch_interaction_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '观看-互动率' AFTER watch_follow_rate,
  ADD COLUMN watch_deal_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '观看-成交率' AFTER watch_interaction_rate,
  ADD COLUMN presale_order_total BIGINT NOT NULL DEFAULT 0 COMMENT '预售累计订单数' AFTER watch_deal_rate,
  ADD COLUMN gmv_delta DECIMAL(18,2) NULL COMMENT '本班成交金额差额' AFTER interaction_delta,
  ADD COLUMN ad_cost_delta DECIMAL(18,2) NULL COMMENT '本班千川消耗差额' AFTER gmv_delta,
  ADD COLUMN presale_order_delta BIGINT NULL COMMENT '本班预售订单差额' AFTER ad_cost_delta;

ALTER TABLE live_metric_shift_summary
  ADD COLUMN gmv_delta DECIMAL(18,2) NULL COMMENT '班次成交金额' AFTER deal_delta,
  ADD COLUMN ad_cost_delta DECIMAL(18,2) NULL COMMENT '班次千川消耗' AFTER gmv_delta,
  ADD COLUMN presale_order_delta BIGINT NULL COMMENT '班次预售订单数' AFTER ad_cost_delta;
