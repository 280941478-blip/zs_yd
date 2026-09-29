CREATE TABLE example_task (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  active_code VARCHAR(32) GENERATED ALWAYS AS (IF(deleted = b'0', code, NULL)) STORED,
  UNIQUE KEY uk_task_code (tenant_id,owner_id,active_code),
  KEY idx_task_owner (tenant_id,owner_id,deleted,id),
  CONSTRAINT chk_task_status CHECK (status BETWEEN 0 AND 2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name)
VALUES (900000,'开发示例','',1,90,0,'/example','ep:document',NULL,NULL),
(900001,'示例任务','example:task:query',2,1,900000,'task','ep:list','example/task/index','ExampleTask'),
(900002,'任务新增','example:task:create',3,1,900001,'', '#',NULL,NULL),
(900003,'任务修改','example:task:update',3,2,900001,'', '#',NULL,NULL),
(900004,'任务删除','example:task:delete',3,3,900001,'', '#',NULL,NULL),
(900005,'任务导出','example:task:export',3,4,900001,'', '#',NULL,NULL),
(900006,'任务导入','example:task:import',3,5,900001,'', '#',NULL,NULL);
INSERT INTO system_role_menu(role_id,menu_id,tenant_id)
SELECT 1,id,1 FROM system_menu WHERE id BETWEEN 900000 AND 900006;
