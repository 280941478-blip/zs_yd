package cn.iocoder.yudao.server.starter;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.example.controller.admin.task.TaskController;
import cn.iocoder.yudao.module.example.controller.admin.task.vo.*;
import cn.iocoder.yudao.module.example.service.TaskService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Run scripts/verify-integration.cjs: independent MySQL/Redis, never the developer database. */
@EnabledIfEnvironmentVariable(named="STARTER_INTEGRATION_TESTS", matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "yudao.access-log.enable=false", "yudao.captcha.enable=false"})
class StarterIntegrationTest {
    @Autowired TaskService tasks;
    @Autowired TaskController controller;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired org.springframework.boot.test.web.client.TestRestTemplate http;
    @BeforeEach void setup() { login(1,1); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }
    private void login(long user,long tenant) {
        LoginUser u=new LoginUser();u.setId(user);u.setUserType(2);u.setTenantId(tenant);
        SecurityFrameworkUtils.setLoginUser(u,new org.springframework.mock.web.MockHttpServletRequest());TenantContextHolder.setTenantId(tenant);
    }
    private TaskSaveReqVO task(String code) {
        var r=new TaskSaveReqVO();r.setCode(code);r.setName("集成验证任务");r.setStatus(0);return r;
    }
    private TaskExcelVO row(String code) {
        var r=new TaskExcelVO();r.setCode(code);r.setName("导入任务");r.setStatus(1);return r;
    }
    @Test void migrationAndRepeatStartupPreserveAccount() {
        String password=jdbc.queryForObject("SELECT password FROM system_users WHERE id=1",String.class);
        assertEquals("2",flyway.info().current().getVersion().getVersion());
        assertEquals(0,flyway.migrate().migrationsExecuted);
        assertEquals(password,jdbc.queryForObject("SELECT password FROM system_users WHERE id=1",String.class));
        assertEquals(7,jdbc.queryForObject("SELECT COUNT(*) FROM system_menu WHERE id BETWEEN 900000 AND 900006",Integer.class));
    }
    @Test void crudAndLogicalDeleteAllowCodeReuse() {
        Long id=tasks.create(task("CRUD"));
        var r=task("CRUD");r.setId(id);r.setStatus(2);tasks.update(r);
        assertEquals(2,tasks.get(id).getStatus());tasks.delete(id);
        assertThrows(ServiceException.class,()->tasks.get(id));
        Long second=tasks.create(task("CRUD"));tasks.delete(second);
        assertNotNull(tasks.create(task("CRUD")));
    }
    @Test void ownerAndTenantIsolationProtectAllOperations() {
        Long id=tasks.create(task("SCOPED"));
        for(long[] identity:List.of(new long[]{2,1},new long[]{1,2})) {
            login(identity[0],identity[1]);
            assertThrows(ServiceException.class,()->tasks.get(id));
            var r=task("SCOPED");r.setId(id);
            assertThrows(ServiceException.class,()->tasks.update(r));
            assertThrows(ServiceException.class,()->tasks.delete(id));
            var query=new TaskPageReqVO();query.setCode("SCOPED");
            assertEquals(0,tasks.page(query).getTotal());assertTrue(tasks.export(query).isEmpty());
        }
        login(1,1);assertEquals("SCOPED",tasks.get(id).getCode());
    }
    @Test void importRollsBackEarlierRowsOnDatabaseConflict() {
        tasks.create(task("EXISTING"));
        assertThrows(ServiceException.class,()->tasks.importRows(List.of(row("ROLLBACK"),row("EXISTING"))));
        var q=new TaskPageReqVO();q.setCode("ROLLBACK");assertEquals(0,tasks.page(q).getTotal());
        assertThrows(ServiceException.class,()->tasks.importRows(List.of(row("DUP"),row("dup"))));
        var invalid=row("INVALID");invalid.setStatus(9);
        assertThrows(ServiceException.class,()->tasks.importRows(List.of(invalid)));
        assertEquals(1,tasks.importRows(List.of(row("VALID"))));
    }
    @Test void serverPermissionsRejectUserWithoutRole() {
        login(99999,1);
        assertThrows(AccessDeniedException.class,()->controller.create(task("FORBIDDEN")));
        assertThrows(AccessDeniedException.class,()->controller.page(new TaskPageReqVO()));
    }
    @Test void realHttpLoginValidationAndExcelRoundTrip() throws Exception {
        var headers=new org.springframework.http.HttpHeaders();headers.set("tenant-id","1");
        var credentials=java.util.Map.of("username","admin","password","VerifyStarter123");
        var login=http.postForObject("/admin-api/system/auth/login",new org.springframework.http.HttpEntity<>(credentials,headers),java.util.Map.class);
        assertEquals(0,login.get("code"));
        headers.setBearerAuth((String)((java.util.Map<?,?>)login.get("data")).get("accessToken"));
        var invalid=task("HTTP_INVALID");invalid.setStatus(8);
        var rejected=http.postForObject("/admin-api/example/task/create",new org.springframework.http.HttpEntity<>(invalid,headers),java.util.Map.class);
        assertNotEquals(0,rejected.get("code"));
        var bytes=new java.io.ByteArrayOutputStream();
        cn.idev.excel.FastExcelFactory.write(bytes,TaskExcelVO.class).autoCloseStream(false).sheet("任务").doWrite(List.of(row("HTTP_IMPORT")));
        var file=new org.springframework.core.io.ByteArrayResource(bytes.toByteArray()) {
            @Override public String getFilename(){return "tasks.xlsx";}
        };
        var body=new org.springframework.util.LinkedMultiValueMap<String,Object>();body.add("file",file);
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
        var imported=http.postForObject("/admin-api/example/task/import-excel",new org.springframework.http.HttpEntity<>(body,headers),java.util.Map.class);
        assertEquals(0,imported.get("code"));assertEquals(1,imported.get("data"));
        headers.remove(org.springframework.http.HttpHeaders.CONTENT_TYPE);
        var exported=http.exchange("/admin-api/example/task/export-excel?code=HTTP_IMPORT",org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<>(headers),byte[].class);
        assertEquals(200,exported.getStatusCode().value());
        var rows=cn.idev.excel.FastExcelFactory.read(new java.io.ByteArrayInputStream(exported.getBody()),TaskExcelVO.class,null).doReadAllSync();
        assertEquals(1,rows.size());assertEquals("HTTP_IMPORT",((TaskExcelVO)rows.get(0)).getCode());
    }
    @Test void legacyAdoptionPreservesPasswordAndRejectsChecksumDrift() throws Exception {
        String schema="starter_legacy_it";
        String baseUrl;
        try(var c=flyway.getConfiguration().getDataSource().getConnection()) {baseUrl=c.getMetaData().getURL();}
        var admin=new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(baseUrl,"root",System.getenv("DB_PASSWORD")));
        admin.execute("CREATE DATABASE "+schema);
        try {
            String url;
            try(var c=flyway.getConfiguration().getDataSource().getConnection()) {
                url=c.getMetaData().getURL().replace("/starter_it?","/"+schema+"?");
            }
            var cfg=Flyway.configure().dataSource(url,"root",System.getenv("DB_PASSWORD"))
                    .locations("classpath:db/migration").cleanDisabled(true);
            var old=cfg.target("1").load();old.migrate();
            var legacy=new JdbcTemplate(old.getConfiguration().getDataSource());
            legacy.update("UPDATE starter_installation SET completed=1");
            legacy.update("UPDATE system_users SET password='changed-by-existing-user' WHERE id=1");
            legacy.execute("DROP TABLE flyway_schema_history");
            var next=Flyway.configure().dataSource(url,"root",System.getenv("DB_PASSWORD"))
                    .locations("classpath:db/migration").cleanDisabled(true).load();
            assertThrows(Exception.class,()->new StarterMigrationConfiguration().starterMigrations(false).migrate(next));
            new StarterMigrationConfiguration().starterMigrations(true).migrate(next);
            assertEquals("2",next.info().current().getVersion().getVersion());
            assertEquals("changed-by-existing-user",legacy.queryForObject("SELECT password FROM system_users WHERE id=1",String.class));
            legacy.update("UPDATE flyway_schema_history SET checksum=123 WHERE version='2'");
            assertThrows(Exception.class,next::migrate);
        } finally { admin.execute("DROP DATABASE "+schema); }
    }
}
