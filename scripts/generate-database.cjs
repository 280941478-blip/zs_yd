// Generate a clean, first-install-only database from the pinned upstream schema.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const sql = fs.readFileSync(path.join(root, 'backend/sql/mysql/ruoyi-vue-pro.sql'), 'utf8');
function values(text) {
  const out=[]; let start=0, quoted=false;
  for(let i=0;i<text.length;i++) {
    if(text[i]==='\\' && quoted) {i++;continue;}
    if(text[i]==="'") {if(quoted && text[i+1]==="'"){i++;continue;} quoted=!quoted;}
    if(text[i]===',' && !quoted){out.push(text.slice(start,i).trim());start=i+1;}
  }
  out.push(text.slice(start).trim()); return out;
}
const rows=[];
for(const m of sql.matchAll(/INSERT INTO `(\w+)` \(([^\n]+?)\) VALUES \(([^\n]+)\);/g)) {
  const cols=[...m[2].matchAll(/`([^`]+)`/g)].map(x=>x[1]); const vals=values(m[3]);
  if(cols.length!==vals.length)throw new Error('Cannot parse '+m[1]);
  rows.push({table:m[1],data:Object.fromEntries(cols.map((c,i)=>[c,vals[i]])),raw:m[0]});
}
const plain=x=>(x||'').replace(/^'|'$/g,'');
const menus=rows.filter(r=>r.table==='system_menu' && r.data.deleted==="b'0'");
const roots=menus.filter(r=>['system','infra'].includes(plain(r.data.path).replace(/^\//,'')) && r.data.parent_id==='0');
const ids=new Set(roots.map(r=>r.data.id));
let prev=-1;while(prev!==ids.size){prev=ids.size;for(const r of menus)if(ids.has(r.data.parent_id))ids.add(r.data.id);}
const schema=[...sql.matchAll(/CREATE TABLE `((?:system|infra)_\w+)`[\s\S]*?;\r?\n/g)].map(m=>m[0].replace(/ AUTO_INCREMENT = \d+/g,''));
if(schema.length<20 || ids.size<30)throw new Error('Unexpected upstream schema/menu structure');
const seed=rows.filter(r=>
  (r.table==='system_menu' && ids.has(r.data.id)) ||
  (['system_dict_type','system_dict_data'].includes(r.table) && r.data.deleted==="b'0'") ||
  (r.table==='system_oauth2_client' && r.data.id==='1')
).map(r=>r.raw);
const custom=`
UPDATE system_oauth2_client SET secret='INITIALIZATION_REQUIRED',name='Cdzs Starter',logo='',description='',redirect_uris='[]',authorized_grant_types='["password","refresh_token"]',scopes='[]',authorities='[]' WHERE id=1;
UPDATE system_menu SET visible=b'0' WHERE component IN ('infra/job/index','infra/druid/index','infra/server/index','infra/swagger/index') OR path LIKE 'http%';
INSERT INTO system_tenant (id,name,contact_user_id,contact_name,package_id,expire_time,account_count,status,websites) VALUES (1,'默认租户',1,'管理员',0,'2099-12-31 23:59:59',10000,0,'[]');
INSERT INTO system_dept (id,name,parent_id,sort,status,tenant_id) VALUES (100,'默认组织',0,0,0,1);
INSERT INTO system_role (id,name,code,sort,data_scope,data_scope_dept_ids,status,type,remark,tenant_id) VALUES (1,'超级管理员','super_admin',0,1,'[]',0,1,'基础版管理员',1),(2,'普通用户','common',1,5,'[]',0,2,'默认无菜单授权，按需分配',1);
INSERT INTO system_users (id,username,password,nickname,dept_id,post_ids,status,tenant_id) VALUES (1,'admin','INITIALIZATION_REQUIRED','系统管理员',100,'[]',0,1);
INSERT INTO system_user_role (user_id,role_id,tenant_id) VALUES (1,1,1);
INSERT INTO system_role_menu (role_id,menu_id,tenant_id) SELECT 1,id,1 FROM system_menu WHERE deleted=b'0';
INSERT INTO infra_config (category,type,name,config_key,value,visible) VALUES ('system',1,'允许自行注册','system.user.register-enabled','false',b'0');
INSERT INTO infra_file_config (id,name,storage,master,config) VALUES (1,'数据库文件存储',1,b'1','{"@class":"cn.cdzs.module.infra.framework.file.core.client.db.DBFileClientConfig","domain":"http://localhost:8080"}');
CREATE TABLE starter_installation (id INT PRIMARY KEY, completed BIT NOT NULL DEFAULT b'0', initialized_at DATETIME NULL);
INSERT INTO starter_installation (id,completed) VALUES (1,b'0');
`;
fs.mkdirSync(path.join(root,'database/init'),{recursive:true});
fs.writeFileSync(path.join(root,'database/init/001-base.sql'),'-- First installation ONLY. No DROP statements. Never import over an existing database.\nSET NAMES utf8mb4;\n'+schema.join('\n')+'\nSTART TRANSACTION;\n'+seed.join('\n')+'\n'+custom+'\nCOMMIT;\n');
console.log(`Generated ${schema.length} tables and ${ids.size} system/infra menu entries; no demo users or messaging credentials.`);
