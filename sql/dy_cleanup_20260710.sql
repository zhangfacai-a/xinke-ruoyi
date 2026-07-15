-- Douyin table cleanup for xinkeerp.
-- Run after backing up dy_capture_batch and dy_plugin_client.

-- Raw capture JSON is write-only in current runtime code and not needed for CRM workflows.
alter table dy_capture_batch drop column raw_payload;

-- Plugin client registry is not referenced by current runtime code.
drop table if exists dy_plugin_client;
