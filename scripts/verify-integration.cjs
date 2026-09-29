// Dedicated disposable services; no .env passwords or host development data are used.
const {spawnSync}=require('node:child_process');
const path=require('node:path');const crypto=require('node:crypto');const fs=require('node:fs');
const root=path.resolve(__dirname,'..');
const suffix=Date.now();const mysql=`starter-it-mysql-${suffix}`, redis=`starter-it-redis-${suffix}`;
const password=crypto.randomBytes(16).toString('hex');
function run(cmd,args,options={}) {
  const r=spawnSync(cmd,args,{cwd:root,encoding:'utf8',...options});
  if(r.status!==0) throw new Error(`${cmd} ${args.slice(0,2).join(' ')} failed: ${r.stderr||r.error||'see verification log'}`);
  return (r.stdout||'').trim();
}
const delay=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
  try {
    const env={...process.env,MYSQL_ROOT_PASSWORD:password};
    run('docker',['run','-d','--name',mysql,'-e','MYSQL_ROOT_PASSWORD','-e','MYSQL_PASSWORD','-e','MYSQL_USER=starter_it','-e','MYSQL_DATABASE=starter_it','-p','127.0.0.1::3306','mysql:8.0.44','--performance-schema=OFF','--innodb-buffer-pool-size=32M'],{env:{...env,MYSQL_PASSWORD:password}});
    run('docker',['run','-d','--name',redis,'-e','MYSQL_ROOT_PASSWORD','-p','127.0.0.1::6379','redis:7.4.2-alpine','sh','-c','exec redis-server --requirepass "$MYSQL_ROOT_PASSWORD"'],{env});
    const dbPort=run('docker',['port',mysql,'3306/tcp']).split(':').pop();
    const redisPort=run('docker',['port',redis,'6379/tcp']).split(':').pop();
    let ready=false;
    for(let i=0;i<90;i++) {
      const r=spawnSync('docker',['exec',mysql,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -e "SELECT 1"'],{encoding:'utf8'});
      if(r.status===0){ready=true;break;} await delay(2000);
    }
    if(!ready)throw Error('Isolated MySQL readiness timeout');
    const log=fs.openSync(path.join(root,'integration-verify.log'),'w');
    try {
      const testEnv={...process.env,JAVA_TOOL_OPTIONS:'-Xms64m -Xmx384m -Dfile.encoding=UTF-8',STARTER_INTEGRATION_TESTS:'true',DB_HOST:'127.0.0.1',DB_PORT:dbPort,DB_NAME:'starter_it',DB_USER:'starter_it',DB_PASSWORD:password,
        REDIS_HOST:'127.0.0.1',REDIS_PORT:redisPort,REDIS_PASSWORD:password,ADMIN_INITIAL_PASSWORD:'VerifyStarter123',APP_PUBLIC_URL:'http://localhost:8080',STARTER_BASELINE_EXISTING:'false'};
      console.log('Running real MySQL/Redis integration tests; output: integration-verify.log');
      // Windows .cmd launching requires cmd.exe; every argument below is a fixed literal.
      const command=process.platform==='win32'?'cmd.exe':'mvn';
      const args=process.platform==='win32'?['/d','/s','/c','mvn -s ../deploy/maven-settings.xml -B -ntp test']:['-s','../deploy/maven-settings.xml','-B','-ntp','test'];
      run(command,args,{cwd:path.join(root,'backend'),env:testEnv,stdio:['ignore',log,log]});
    } finally {fs.closeSync(log);}
    console.log('PASS: fresh migration, repeat migration, CRUD, tenant/owner isolation, permissions and atomic import.');
  } finally {
    for(const name of [mysql,redis])spawnSync('docker',['rm','-fv',name],{stdio:'ignore'});
  }
})().catch(e=>{console.error(e.message);process.exitCode=1;});
