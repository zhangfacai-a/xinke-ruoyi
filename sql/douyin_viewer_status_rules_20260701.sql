-- Douyin viewer lead status rules:
-- 1. order_no exists -> ordered
-- 2. no order_no and lead is older than one month -> invalid
-- 3. no order_no and owner_name exists -> following
-- 4. otherwise -> new
-- Leads with comments are always high intent.

update dy_viewer_daily_lead
set status = case
        when order_no is not null and trim(order_no) != '' then 'ordered'
        when lead_date < date_sub(curdate(), interval 1 month) then 'invalid'
        when owner_name is not null and trim(owner_name) != '' then 'following'
        else 'new'
    end,
    intent = case
        when has_comment = 1 then 'high'
        else intent
    end,
    update_time = sysdate()
where status != case
        when order_no is not null and trim(order_no) != '' then 'ordered'
        when lead_date < date_sub(curdate(), interval 1 month) then 'invalid'
        when owner_name is not null and trim(owner_name) != '' then 'following'
        else 'new'
    end
   or (has_comment = 1 and (intent is null or intent != 'high'));
