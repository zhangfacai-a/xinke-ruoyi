-- Douyin URL cleanup for xinkeerp.
-- These fields are diagnostic-only and are not read by current CRM runtime code.

alter table dy_capture_batch drop column page_url;
alter table dy_capture_batch drop column request_url;
alter table dy_viewer_comment drop column page_url;
