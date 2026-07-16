ALTER TABLE dy_plugin_client
  MODIFY COLUMN request_url VARCHAR(1000) DEFAULT NULL COMMENT 'Last request url';

ALTER TABLE dy_capture_batch
  MODIFY COLUMN request_url VARCHAR(1000) DEFAULT NULL COMMENT 'Request URL';
