-- Business entry: only the default administrator receives the initial menu grant.
INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name)
VALUES (901000,'业务管理','',1,80,0,'/business','ep:briefcase',NULL,NULL),
       (901001,'业务首页','business:home:query',2,1,901000,'home','ep:house','business/home/index','BusinessHome');
INSERT INTO system_role_menu(role_id,menu_id,tenant_id)
SELECT 1,id,1 FROM system_menu WHERE id IN (901000,901001);
