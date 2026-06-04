package persistence;

import org.flywaydb.core.Flyway;

public final class DatabaseMigrator {
    private DatabaseMigrator() {
    }

    public static void migrate() {
        Flyway.configure()
                .dataSource(DatabaseConfig.jdbcUrl(), DatabaseConfig.username(), DatabaseConfig.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
