package cn.cdzs.server.starter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Explicit adoption of the pre-Flyway starter only; never silently baseline an arbitrary database. */
@Configuration(proxyBeanMethods = false)
public class StarterMigrationConfiguration {
    @Bean
    FlywayMigrationStrategy starterMigrations(
            @Value("${STARTER_BASELINE_EXISTING:false}") boolean adoptExisting) {
        return flyway -> {
            if (adoptExisting && flyway.info().all().length > 0 && flyway.info().current() == null) {
                var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
                Integer marker = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema=DATABASE() AND table_name='starter_installation'", Integer.class);
                if (marker != null && marker > 0) {
                    if (!Boolean.TRUE.equals(jdbc.queryForObject(
                            "SELECT completed FROM starter_installation WHERE id=1", Boolean.class))) {
                        throw new IllegalStateException("Only an initialized legacy starter can be baselined");
                    }
                    Integer admin = jdbc.queryForObject("SELECT COUNT(*) FROM system_users "
                            + "WHERE id=1 AND tenant_id=1 AND username='admin' AND deleted=0", Integer.class);
                    if (admin == null || admin != 1) throw new IllegalStateException("Legacy starter admin is missing");
                    // These core columns must exist; a different schema fails rather than being silently adopted.
                    jdbc.queryForList("SELECT config FROM infra_file_config WHERE id=1");
                    jdbc.queryForList("SELECT permission,component FROM system_menu LIMIT 1");
                    flyway.baseline(); // version 1 is the immutable original starter schema
                }
            }
            flyway.migrate();
        };
    }
}
