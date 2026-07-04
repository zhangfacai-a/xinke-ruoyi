-- Repair wrong Douyin live room key caused by audience response room_id overriding report roomKey.
-- Expected room key: 1956768266328364
-- Wrong room key:    7657357850490047241

set @right_room_key := '1956768266328364';
set @wrong_room_key := '7657357850490047241';

update dy_capture_batch
set room_key = @right_room_key
where room_key = @wrong_room_key;

update dy_viewer_comment
set room_key = @right_room_key
where room_key = @wrong_room_key;

update dy_viewer_daily_lead
set room_key = @right_room_key,
    update_time = sysdate()
where room_key = @wrong_room_key;

update dy_viewer_comment c
join dy_viewer_daily_lead l
  on l.lead_date = c.lead_date
 and l.room_key = c.room_key
 and l.nickname = c.nickname
set c.lead_id = l.lead_id,
    c.viewer_id = l.viewer_id,
    c.sec_uid = l.sec_uid,
    c.match_type = 'nickname'
where c.room_key = @right_room_key
  and c.lead_id is null;

update dy_viewer_daily_lead l
set has_comment = if((select count(1) from dy_viewer_comment c where c.lead_id = l.lead_id) > 0, 1, 0),
    comment_count = (select count(1) from dy_viewer_comment c where c.lead_id = l.lead_id),
    intent = if((select count(1) from dy_viewer_comment c where c.lead_id = l.lead_id) > 0, 'high', intent),
    first_comment_time = (select min(c.captured_at) from dy_viewer_comment c where c.lead_id = l.lead_id),
    last_comment_time = (select max(c.captured_at) from dy_viewer_comment c where c.lead_id = l.lead_id),
    last_comment_content = (
      select c.content
      from dy_viewer_comment c
      where c.lead_id = l.lead_id
      order by c.captured_at desc, c.comment_id desc
      limit 1
    ),
    update_time = sysdate()
where l.room_key = @right_room_key;

delete from dy_live_room
where room_key = @wrong_room_key;
