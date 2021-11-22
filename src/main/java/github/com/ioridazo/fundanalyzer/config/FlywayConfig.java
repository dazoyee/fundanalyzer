package github.com.ioridazo.fundanalyzer.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;

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
            log.info(MessageFormat.format(
                    "=== Flyway info ===\n" +
                            "Version: {0}\n" +
                            "Description: {1}\n" +
                            "Type: {2}\n" +
                            "Script: {3}\n" +
                            "Checksum: {4}\n" +
                            "Installed on: {5}\n" +
                            "Execution time: {6}\n" +
                            "State: {7}\n",
                    info.getVersion().getVersion(),
                    info.getDescription(),
                    info.getType().name(),
                    info.getScript(),
                    info.getChecksum(),
                    info.getInstalledOn(),
                    info.getExecutionTime(),
                    info.getState().getDisplayName()
            ));
    }
}
