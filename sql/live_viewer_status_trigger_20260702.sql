-- Keep live viewer lead status consistent even when older backend builds update only owner/order fields.
-- Rule: status=pre_ordered + order_no -> pre_ordered; other order_no -> ordered;
-- older than one month -> invalid; owner_name -> following; otherwise new.

drop trigger if exists trg_dy_viewer_status_bi;
drop trigger if exists trg_dy_viewer_status_bu;
drop trigger if exists trg_dy_viewer_daily_lead_status_bi;
drop trigger if exists trg_dy_viewer_daily_lead_status_bu;

create trigger trg_dy_viewer_status_bi
before insert on dy_viewer
for each row
set NEW.status = case
        when NEW.order_no is not null and trim(NEW.order_no) <> '' and NEW.status = 'pre_ordered' then 'pre_ordered'
        when NEW.order_no is not null and trim(NEW.order_no) <> '' then 'ordered'
        when NEW.last_seen_date is not null and NEW.last_seen_date < date_sub(curdate(), interval 1 month) then 'invalid'
        when NEW.owner_name is not null and trim(NEW.owner_name) <> '' then 'following'
        else 'new'
    end,
    NEW.intent = case
        when ifnull(NEW.has_comment, 0) = 1 then 'high'
        else NEW.intent
    end;

create trigger trg_dy_viewer_status_bu
before update on dy_viewer
for each row
set NEW.status = case
        when NEW.order_no is not null and trim(NEW.order_no) <> '' and NEW.status = 'pre_ordered' then 'pre_ordered'
        when NEW.order_no is not null and trim(NEW.order_no) <> '' then 'ordered'
        when NEW.last_seen_date is not null and NEW.last_seen_date < date_sub(curdate(), interval 1 month) then 'invalid'
        when NEW.owner_name is not null and trim(NEW.owner_name) <> '' then 'following'
        else 'new'
    end,
    NEW.intent = case
        when ifnull(NEW.has_comment, 0) = 1 then 'high'
        else NEW.intent
    end;

create trigger trg_dy_viewer_daily_lead_status_bi
before insert on dy_viewer_daily_lead
for each row
set NEW.status = case
        when NEW.order_no is not null and trim(NEW.order_no) <> '' and NEW.status = 'pre_ordered' then 'pre_ordered'
        when NEW.order_no is not null and trim(NEW.order_no) <> '' then 'ordered'
        when NEW.lead_date is not null and NEW.lead_date < date_sub(curdate(), interval 1 month) then 'invalid'
        when NEW.owner_name is not null and trim(NEW.owner_name) <> '' then 'following'
        else 'new'
    end,
    NEW.intent = case
        when ifnull(NEW.has_comment, 0) = 1 then 'high'
        else NEW.intent
    end;

create trigger trg_dy_viewer_daily_lead_status_bu
before update on dy_viewer_daily_lead
for each row
set NEW.status = case
        when NEW.order_no is not null and trim(NEW.order_no) <> '' and NEW.status = 'pre_ordered' then 'pre_ordered'
        when NEW.order_no is not null and trim(NEW.order_no) <> '' then 'ordered'
        when NEW.lead_date is not null and NEW.lead_date < date_sub(curdate(), interval 1 month) then 'invalid'
        when NEW.owner_name is not null and trim(NEW.owner_name) <> '' then 'following'
        else 'new'
    end,
    NEW.intent = case
        when ifnull(NEW.has_comment, 0) = 1 then 'high'
        else NEW.intent
    end;
