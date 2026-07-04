-- Fix live viewer lead filters and intent rules.

update dy_viewer_daily_lead
set intent = 'high'
where has_comment = 1;
