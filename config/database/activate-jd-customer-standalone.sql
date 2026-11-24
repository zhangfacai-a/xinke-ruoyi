-- Production serves this page as a standalone UI bundle under the existing site.
UPDATE sys_menu SET path='http://47.116.62.234/jd-customers-app/jd-customers.html',component=NULL,is_frame=1,
    menu_name='京东客户信息',perms='',status='0',visible='0'
WHERE component='live/jd-customers/index' AND menu_type='C';

SELECT menu_id,menu_name,path FROM sys_menu
WHERE path='http://47.116.62.234/jd-customers-app/jd-customers.html' AND menu_type='C';
