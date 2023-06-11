package github.com.ioridazo.fundanalyzer.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final Logger log = LogManager.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.migrate();
            log(flyway);
        };
    }

    private void log(final Flyway flyway) {
        for (MigrationInfo info : flyway.info().all())
            log.info("""
                            === Flyway info ===
                            Version: {}
                            Description: {}
                            Type: {}
                            Script: {}
                            Checksum: {}
                            Installed on: {}
                            Execution time: {}
                            State: {}
                            """,
                    info.getVersion().getVersion(),
                    info.getDescription(),
                    info.getType().name(),
                    info.getScript(),
                    info.getChecksum(),
                    info.getInstalledOn(),
                    info.getExecutionTime(),
                    info.getState().getDisplayName()
            );
    }
}
