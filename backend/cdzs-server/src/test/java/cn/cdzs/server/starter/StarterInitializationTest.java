package cn.cdzs.server.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class StarterInitializationTest {
    JdbcTemplate jdbc; StarterInitialization runner;
    BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE starter_installation(id INT PRIMARY KEY,completed BOOLEAN,initialized_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE system_users(id INT PRIMARY KEY,tenant_id BIGINT,password VARCHAR(255))");
        jdbc.execute("CREATE TABLE infra_file_config(id INT PRIMARY KEY,config VARCHAR(4096))");
        jdbc.execute("CREATE TABLE system_oauth2_client(id INT PRIMARY KEY,secret VARCHAR(255),name VARCHAR(64),logo VARCHAR(255),description VARCHAR(255),redirect_uris VARCHAR(255),authorized_grant_types VARCHAR(255),scopes VARCHAR(255),authorities VARCHAR(255))");
        jdbc.update("INSERT INTO system_oauth2_client(id,secret) VALUES(1,'INITIALIZATION_REQUIRED')");
        jdbc.update("INSERT INTO starter_installation VALUES(1,false,null)");
        jdbc.update("INSERT INTO system_users VALUES(1,1,'INITIALIZATION_REQUIRED')");
        jdbc.update("INSERT INTO infra_file_config VALUES(1,'{}')");
        runner=new StarterInitialization(jdbc,new TransactionTemplate(new DataSourceTransactionManager(ds)),encoder,new ObjectMapper());
        ReflectionTestUtils.setField(runner,"password","starter-test-123");
        ReflectionTestUtils.setField(runner,"publicUrl","https://starter.example.com");
    }
    @Test void redisReadsLegacyClassMetadataWithoutChangingUserText() {
        var serializer = (org.springframework.data.redis.serializer.RedisSerializer<Object>)
                cn.cdzs.framework.redis.config.CdzsRedisAutoConfiguration.buildRedisSerializer();
        var user = new cn.cdzs.framework.security.core.LoginUser();
        user.setId(42L);
        user.setInfo(java.util.Map.of("nickname", "cn.iocoder.yudao.keep-this-text"));
        String json = new String(serializer.serialize(user), java.nio.charset.StandardCharsets.UTF_8)
                .replace("cn.cdzs.framework.security.core.LoginUser", "cn.iocoder.yudao.framework.security.core.LoginUser");
        var restored = (cn.cdzs.framework.security.core.LoginUser) serializer.deserialize(
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(42L, restored.getId());
        assertEquals("cn.iocoder.yudao.keep-this-text", restored.getInfo().get("nickname"));
    }
    @Test void initializesPasswordAndFileDomain() {
        runner.run(null);
        assertTrue(encoder.matches("starter-test-123",jdbc.queryForObject("SELECT password FROM system_users WHERE id=1",String.class)));
        assertTrue(jdbc.queryForObject("SELECT completed FROM starter_installation",Boolean.class));
        assertTrue(jdbc.queryForObject("SELECT config FROM infra_file_config",String.class).contains("https://starter.example.com"));
    }
    @Test void restartDoesNotResetChangedPassword() {
        runner.run(null);jdbc.update("UPDATE system_users SET password='changed-by-user'");
        ReflectionTestUtils.setField(runner,"password","");runner.run(null);
        assertEquals("changed-by-user",jdbc.queryForObject("SELECT password FROM system_users",String.class));
    }
    @Test void rejectsWeakInitialPassword() {
        ReflectionTestUtils.setField(runner,"password","admin123");
        assertThrows(IllegalStateException.class,()->runner.run(null));
        assertFalse(jdbc.queryForObject("SELECT completed FROM starter_installation",Boolean.class));
    }
    @Test void invalidDomainDoesNotPartiallyInitialize() {
        ReflectionTestUtils.setField(runner,"publicUrl","file:///tmp");
        assertThrows(IllegalArgumentException.class,()->runner.run(null));
        assertEquals("INITIALIZATION_REQUIRED",jdbc.queryForObject("SELECT password FROM system_users",String.class));
        assertFalse(jdbc.queryForObject("SELECT completed FROM starter_installation",Boolean.class));
    }
}
