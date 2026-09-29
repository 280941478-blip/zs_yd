package cn.iocoder.yudao.server.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;

/** Initializes a fresh installation once; never resets existing accounts on restart. */
@Component
public class StarterInitialization implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final PasswordEncoder encoder;
    private final ObjectMapper mapper;
    @Value("${ADMIN_INITIAL_PASSWORD:}") private String password;
    @Value("${APP_PUBLIC_URL:http://localhost:8080}") private String publicUrl;

    public StarterInitialization(JdbcTemplate jdbc, TransactionTemplate transactions,
                                 PasswordEncoder encoder, ObjectMapper mapper) {
        this.jdbc=jdbc; this.transactions=transactions; this.encoder=encoder; this.mapper=mapper;
    }
    @Override public void run(ApplicationArguments args) {
        transactions.executeWithoutResult(status -> {
            Boolean initialized=jdbc.queryForObject("SELECT completed FROM starter_installation WHERE id=1 FOR UPDATE",Boolean.class);
            if(Boolean.TRUE.equals(initialized)) return;
            if(password.length()<12 || password.length()>16) throw new IllegalStateException("首次启动请设置12至16位 ADMIN_INITIAL_PASSWORD（运行 scripts/init-env.cjs）");
            try {
                var uri=java.net.URI.create(publicUrl);
                if(!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null) throw new IllegalArgumentException("APP_PUBLIC_URL 必须是有效 http(s) 地址");
                jdbc.update("UPDATE system_users SET password=? WHERE id=1 AND tenant_id=1",encoder.encode(password));
                jdbc.update("UPDATE system_oauth2_client SET secret=?,name='Yudao Starter',logo='',description='',redirect_uris='[]',authorized_grant_types='[\"password\",\"refresh_token\"]',scopes='[]',authorities='[]' WHERE id=1",java.util.UUID.randomUUID().toString());
                String config=mapper.writeValueAsString(Map.of("@class","cn.iocoder.yudao.module.infra.framework.file.core.client.db.DBFileClientConfig","domain",publicUrl.replaceAll("/+$","")));
                jdbc.update("UPDATE infra_file_config SET config=? WHERE id=1",config);
                jdbc.update("UPDATE starter_installation SET completed=?,initialized_at=NOW() WHERE id=1",true);
            } catch(java.io.IOException e) {throw new IllegalStateException("初始化文件配置失败",e);}
        });
    }
}
