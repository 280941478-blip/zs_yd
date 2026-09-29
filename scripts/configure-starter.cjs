const fs=require('node:fs');const path=require('node:path');const root=path.resolve(__dirname,'..');
const write=(p,s)=>{fs.mkdirSync(path.dirname(path.join(root,p)),{recursive:true});fs.writeFileSync(path.join(root,p),s)};
write('backend/cdzs-server/src/main/resources/application.yaml',`spring:
  application:
    name: cdzs-starter
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:starter}
  main:
    allow-circular-references: true
  servlet:
    multipart:
      max-file-size: 16MB
      max-request-size: 32MB
  jackson:
    serialization:
      write-dates-as-timestamps: true
      write-date-timestamps-as-nanoseconds: false
      write-durations-as-timestamps: true
      fail-on-empty-beans: false
  cache:
    type: REDIS
    redis:
      time-to-live: 1h
  data:
    redis:
      repositories:
        enabled: false
server:
  port: 48080
  shutdown: graceful
  servlet:
    encoding:
      charset: UTF-8
      force: true
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: NONE
      logic-delete-value: 1
      logic-not-delete-value: 0
    banner: false
  type-aliases-package: \${cdzs.info.base-package}.module.*.dal.dataobject
mybatis-plus-join:
  banner: false
easy-trans:
  is-enable-global: false
springdoc:
  api-docs:
    enabled: \${API_DOCS_ENABLED:false}
  swagger-ui:
    enabled: \${API_DOCS_ENABLED:false}
knife4j:
  enable: \${API_DOCS_ENABLED:false}
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
aj:
  captcha:
    cache-type: redis
    type: blockPuzzle
    water-mark: CDZS Starter
    interference-options: 0
    req-frequency-limit-enable: true
cdzs:
  info:
    version: 1.0.0
    base-package: cn.cdzs
  web:
    admin-ui:
      url: \${APP_PUBLIC_URL:http://localhost:8080}
  security:
    mock-enable: false
    password-encoder-length: 10
    permit-all-urls:
      - /actuator/health
      - /actuator/health/**
  captcha:
    enable: true
  demo: false
  access-log:
    enable: true
  api-encrypt:
    enable: false
  websocket:
    enable: true
    path: /infra/ws
    sender-type: local
  swagger:
    title: CDZS Starter
    description: 通用项目基础版
    version: 1.0.0
    url: \${cdzs.web.admin-ui.url}
    email: admin@example.invalid
    license: MIT
    license-url: https://opensource.org/licenses/MIT
  codegen:
    base-package: cn.cdzs
    db-schemas: \${DB_NAME:cdzs_starter}
    front-type: 20
    vo-type: 10
    delete-batch-enable: true
    unit-test-enable: false
    import-enable: false
  tenant:
    enable: true
    ignore-visit-urls:
      - /admin-api/system/user/profile/**
      - /admin-api/system/auth/**
    ignore-tables:
      - starter_installation
    ignore-caches:
      - user_role_ids
      - permission_menu_ids
      - oauth_client
      - notify_template
      - mail_account
      - mail_template
      - sms_template
  sms-code:
    expire-times: 10m
    send-frequency: 1m
    send-maximum-quantity-per-day: 10
    begin-code: 100000
    end-code: 999999
justauth:
  enabled: false
wx:
  mp:
    app-id: starter-not-configured
    secret: starter-not-configured
  miniapp:
    appid: starter-not-configured
    secret: starter-not-configured
`);
write('backend/cdzs-server/src/main/resources/application-starter.yaml',`spring:
  config:
    import: "optional:file:../.env[.properties],optional:file:./.env[.properties]"
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
  datasource:
    druid:
      web-stat-filter:
        enabled: false
      stat-view-servlet:
        enabled: false
    dynamic:
      primary: master
      strict: true
      datasource:
        master:
          name: \${DB_NAME:cdzs_starter}
          url: jdbc:mysql://\${DB_HOST:127.0.0.1}:\${DB_PORT:13306}/\${DB_NAME:cdzs_starter}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true
          username: \${DB_USER:cdzs}
          password: \${DB_PASSWORD}
      druid:
        initial-size: 1
        min-idle: 1
        max-active: 20
        validation-query: SELECT 1
  data:
    redis:
      host: \${REDIS_HOST:127.0.0.1}
      port: \${REDIS_PORT:16379}
      password: \${REDIS_PASSWORD}
      database: 0
  boot:
    admin:
      client:
        enabled: false
      server:
        enabled: false
logging:
  file:
    name: \${LOG_PATH:./logs}/application.log
`);
// Replace upstream environment examples so changing profile never activates demo credentials.
for(const profile of ['local','dev'])write('backend/cdzs-server/src/main/resources/application-'+profile+'.yaml','spring:\n  config:\n    import: classpath:application-starter.yaml\n');
write('frontend/.env',`VITE_APP_TITLE=CDZS Starter
VITE_PORT=8080
VITE_OPEN=false
VITE_APP_TENANT_ENABLE=true
VITE_APP_CAPTCHA_ENABLE=true
VITE_APP_DOCALERT_ENABLE=false
VITE_APP_DEFAULT_LOGIN_TENANT=默认租户
VITE_APP_DEFAULT_LOGIN_USERNAME=admin
VITE_APP_DEFAULT_LOGIN_PASSWORD=
VITE_APP_BAIDU_CODE=
VITE_BAIDU_MAP_KEY=
VITE_APP_API_ENCRYPT_ENABLE=false
VITE_APP_API_ENCRYPT_HEADER=X-Api-Encrypt
VITE_APP_API_ENCRYPT_ALGORITHM=AES
`);
const env=`VITE_BASE_URL=
VITE_API_URL=/admin-api
VITE_UPLOAD_TYPE=server
VITE_BASE_PATH=/
VITE_SOURCEMAP=false
VITE_COMPRESS=none
VITE_DROP_DEBUGGER=true
VITE_DROP_CONSOLE=true
VITE_APP_CAPTCHA_ENABLE=true
`;
for(const p of ['starter','deploy','local','env.local','dev','test','stage','prod'])write('frontend/.env.'+p,env);
const pkgPath=path.join(root,'frontend/package.json');const pkg=JSON.parse(fs.readFileSync(pkgPath));pkg.name='cdzs-starter-admin';pkg.private=true;pkg.packageManager='pnpm@10.33.0';pkg.scripts.dev='vite --mode starter';pkg.scripts.build='node --max_old_space_size=8192 ./node_modules/vite/bin/vite.js build --mode deploy';fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n');
let vite=fs.readFileSync(path.join(root,'frontend/vite.config.ts'),'utf8');vite=vite.replace('host: "0.0.0.0",','host: "127.0.0.1",\n            proxy: {\n                "/admin-api": { target: "http://127.0.0.1:48080", changeOrigin: true },\n                "/app-api": { target: "http://127.0.0.1:48080", changeOrigin: true },\n                "/infra/ws": { target: "ws://127.0.0.1:48080", ws: true }\n            },');write('frontend/vite.config.ts',vite);
write('frontend/src/views/Home/Index.vue',`<template>
  <ContentWrap title="项目基础版">
    <el-alert title="基础能力已就绪，可以开始开发业务模块" type="success" :closable="false" />
    <el-descriptions :column="1" border class="mt-20px">
      <el-descriptions-item label="组织与权限">用户、部门、岗位、角色、菜单、按钮及数据权限</el-descriptions-item>
      <el-descriptions-item label="消息">站内信可直接配置；短信与邮件需先配置自己的渠道和模板</el-descriptions-item>
      <el-descriptions-item label="审计">登录日志、操作日志、接口访问与异常日志</el-descriptions-item>
      <el-descriptions-item label="开发">字典、参数、文件管理、代码生成</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>
<script setup lang="ts">
defineOptions({ name: 'Index' })
</script>
`);
console.log('Starter configuration written');
