-- Keep the deployed UI intact: render the dedicated freight page as an internal link.
UPDATE sys_menu
SET path='http://47.116.62.234/freight-app/freight.html', component=NULL, is_frame=1
WHERE component='live/freight/index' AND menu_type='C'
  AND parent_id=(SELECT menu_id FROM (SELECT menu_id FROM sys_menu
    WHERE path='live-ops' AND menu_type='M' AND status='0') parent_menu)
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE path='http://47.116.62.234/freight-app/freight.html' AND menu_type='C') existing_menu);

SELECT menu_id,menu_name,path,component,is_frame,perms FROM sys_menu
WHERE path='http://47.116.62.234/freight-app/freight.html';
