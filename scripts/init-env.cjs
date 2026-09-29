const fs=require('node:fs'),crypto=require('node:crypto'),path=require('node:path');
const root=path.resolve(__dirname,'..'),target=path.join(root,'.env');
if(fs.existsSync(target)){console.log('.env 已存在，保留原配置。');process.exit(0);}
let text=fs.readFileSync(path.join(root,'.env.example'),'utf8');
for(const key of ['MYSQL_ROOT_PASSWORD','DB_PASSWORD','REDIS_PASSWORD','ADMIN_INITIAL_PASSWORD'])text=text.replace(new RegExp('^'+key+'=.*$','m'),key+'='+crypto.randomBytes(key==='ADMIN_INITIAL_PASSWORD'?8:18).toString('hex'));
fs.writeFileSync(target,text,{mode:0o600});console.log('已生成 .env（含随机数据库、Redis及管理员初始密码）。请在本地查看，不要提交到Git。');
