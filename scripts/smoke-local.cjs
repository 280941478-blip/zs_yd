// Local test only: start backend with --cdzs.captcha.enable=false; stop it after the test.
// Does not print passwords or tokens and never sends SMS/email/notifications.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const env=Object.fromEntries(fs.readFileSync(path.join(__dirname,'../.env'),'utf8').split(/\r?\n/).filter(l=>l&&!l.startsWith('#')).map(l=>{const i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
const base='http://127.0.0.1:48080';
async function call(url,body,token){const res=await fetch(base+url,{method:body?'POST':'GET',headers:{'Content-Type':'application/json','tenant-id':'1',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});return res.json();}
(async()=>{
  const health=await call('/actuator/health');assert.equal(health.status,'UP');
  const anonymous=await call('/admin-api/system/user/page?pageNo=1&pageSize=10');assert.equal(anonymous.code,401);
  const login=await call('/admin-api/system/auth/login',{username:'admin',password:env.ADMIN_INITIAL_PASSWORD});
  assert.equal(login.code,0,login.msg);assert.ok(login.data.accessToken);const token=login.data.accessToken;
  try {
    for(const url of [
      '/admin-api/system/auth/get-permission-info',
      '/admin-api/system/user/page?pageNo=1&pageSize=10',
      '/admin-api/system/role/page?pageNo=1&pageSize=10',
      '/admin-api/system/dept/list',
      '/admin-api/system/menu/list',
      '/admin-api/system/notify-template/page?pageNo=1&pageSize=10',
      '/admin-api/system/sms-channel/page?pageNo=1&pageSize=10',
      '/admin-api/system/mail-account/page?pageNo=1&pageSize=10',
      '/admin-api/system/login-log/page?pageNo=1&pageSize=10',
      '/admin-api/system/operate-log/page?pageNo=1&pageSize=10'
    ]){const r=await call(url,null,token);assert.equal(r.code,0,url+': '+r.msg);console.log('PASS '+url.split('?')[0]);}
    const form=new FormData();form.append('file',new Blob(['starter-local-smoke'],{type:'text/plain'}),'starter-smoke.txt');
    const r=await fetch(base+'/admin-api/infra/file/upload',{method:'POST',headers:{'tenant-id':'1',Authorization:'Bearer '+token},body:form}).then(r=>r.json());
    assert.equal(r.code,0,r.msg);assert.ok(r.data);console.log('PASS file upload (test file retained in local test DB)');
  } finally {await call('/admin-api/system/auth/logout',{},token);}
  console.log('Local smoke passed: login, authentication, 10 admin APIs, file upload.');
})().catch(e=>{console.error(e.message);process.exitCode=1;});
