-- 在发布新版代码后执行；执行前备份 live_retention_order、live_retention_history。
-- 合并旧电话到电话地址，保留旧描述到最新跟进，然后删除两个废弃字段。
-- MySQL DDL 会自动提交；保留备份用于恢复。脚本可重复执行。
DROP PROCEDURE IF EXISTS cleanup_retention_legacy_columns;
DELIMITER $$
CREATE PROCEDURE cleanup_retention_legacy_columns()
BEGIN
  DECLARE col_count INT;
  DECLARE too_long INT;
  SELECT COUNT(*) INTO col_count FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='live_retention_order' AND column_name='phone';
  IF col_count > 0 THEN
    SELECT COUNT(*) INTO too_long FROM live_retention_order
    WHERE CHAR_LENGTH(TRIM(CONCAT_WS(' ',NULLIF(TRIM(phone),''),NULLIF(TRIM(address),''))))>500;
    IF too_long > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Combined contact exceeds 500 characters; migration stopped';
    END IF;
    UPDATE live_retention_order
    SET address=TRIM(CONCAT_WS(' ',NULLIF(TRIM(phone),''),NULLIF(TRIM(address),''))),phone=''
    WHERE TRIM(phone)<>'';
    ALTER TABLE live_retention_order DROP COLUMN phone;
  END IF;
  SELECT COUNT(*) INTO col_count FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='live_retention_order' AND column_name='description';
  IF col_count > 0 THEN
    UPDATE live_retention_order SET latest_note=description WHERE latest_note='' AND description<>'';
    ALTER TABLE live_retention_order DROP COLUMN description;
  END IF;
END$$
DELIMITER ;
CALL cleanup_retention_legacy_columns();
DROP PROCEDURE IF EXISTS cleanup_retention_legacy_columns;
