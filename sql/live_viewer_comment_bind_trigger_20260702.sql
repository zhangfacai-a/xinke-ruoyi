-- Bind incoming Douyin live comments to viewer leads even when room_key drifts.
-- This protects the online database while an older backend still stores
-- audiences with Douyin response room_id but comments with plugin roomKey.

drop trigger if exists trg_dy_viewer_comment_bi;
drop trigger if exists trg_dy_viewer_comment_ai;

delimiter //

create trigger trg_dy_viewer_comment_bi
before insert on dy_viewer_comment
for each row
begin
    if NEW.lead_id is null and NEW.sec_uid is not null and trim(NEW.sec_uid) <> '' then
        set NEW.lead_id = (
            select l.lead_id
            from dy_viewer_daily_lead l
            where l.lead_date = NEW.lead_date
              and l.room_key = NEW.room_key
              and l.sec_uid = NEW.sec_uid
            order by l.update_time desc, l.lead_id desc
            limit 1
        );
        if NEW.lead_id is not null then
            set NEW.match_type = 'sec_uid';
        end if;
    end if;

    if NEW.lead_id is null and NEW.nickname is not null and trim(NEW.nickname) <> '' then
        set NEW.lead_id = (
            select l.lead_id
            from dy_viewer_daily_lead l
            where l.lead_date = NEW.lead_date
              and l.room_key = NEW.room_key
              and l.nickname = NEW.nickname
            order by l.update_time desc, l.lead_id desc
            limit 1
        );
        if NEW.lead_id is not null then
            set NEW.match_type = 'nickname';
        end if;
    end if;

    if NEW.lead_id is null and NEW.nickname is not null and trim(NEW.nickname) <> '' then
        set NEW.lead_id = (
            select l.lead_id
            from dy_viewer_daily_lead l
            where l.lead_date = NEW.lead_date
              and l.nickname = NEW.nickname
            order by l.update_time desc, l.lead_id desc
            limit 1
        );
        if NEW.lead_id is not null then
            set NEW.match_type = 'nickname_any_room';
        end if;
    end if;

    if NEW.lead_id is not null and NEW.viewer_id is null then
        set NEW.viewer_id = (
            select l.viewer_id
            from dy_viewer_daily_lead l
            where l.lead_id = NEW.lead_id
            limit 1
        );
    end if;
end//

create trigger trg_dy_viewer_comment_ai
after insert on dy_viewer_comment
for each row
begin
    if NEW.lead_id is not null then
        update dy_viewer_daily_lead l
        set l.has_comment = 1,
            l.comment_count = (
                select count(1)
                from dy_viewer_comment c
                where c.lead_id = NEW.lead_id
            ),
            l.intent = 'high',
            l.first_comment_time = (
                select min(c.captured_at)
                from dy_viewer_comment c
                where c.lead_id = NEW.lead_id
            ),
            l.last_comment_time = (
                select max(c.captured_at)
                from dy_viewer_comment c
                where c.lead_id = NEW.lead_id
            ),
            l.last_comment_content = NEW.content,
            l.update_time = sysdate()
        where l.lead_id = NEW.lead_id;
    end if;

    if NEW.viewer_id is not null then
        update dy_viewer v
        set v.has_comment = 1,
            v.comment_count = (
                select count(1)
                from dy_viewer_comment c
                where c.viewer_id = NEW.viewer_id
            ),
            v.intent = 'high',
            v.first_comment_time = (
                select min(c.captured_at)
                from dy_viewer_comment c
                where c.viewer_id = NEW.viewer_id
            ),
            v.last_comment_time = (
                select max(c.captured_at)
                from dy_viewer_comment c
                where c.viewer_id = NEW.viewer_id
            ),
            v.last_comment_content = NEW.content,
            v.update_time = sysdate()
        where v.viewer_id = NEW.viewer_id;
    end if;
end//

delimiter ;
