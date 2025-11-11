package com.icefx.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Centralized database configuration using HikariCP. Supports MySQL (default)
 * and SQLite (portable deployments) with settings sourced from {@link AppConfig}.
 */
public final class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";

    private static HikariDataSource dataSource;

    static {
        initializeDataSource();
    }

    private DatabaseConfig() {
        // Utility class
    }

    private static synchronized void initializeDataSource() {
        closePool();

        try {
            AppConfig.initialize();
            String dbType = AppConfig.getDatabaseType().toLowerCase();

            HikariConfig config = new HikariConfig();

            if ("sqlite".equals(dbType)) {
                configureSQLite(config);
            } else {
                configureMySql(config);
            }

            applyPoolSettings(config, "sqlite".equals(dbType));
            dataSource = new HikariDataSource(config);
            logger.info("✅ Database connection pool initialized ({})", config.getPoolName());

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void configureMySql(HikariConfig config) {
        String host = AppConfig.get("db.mysql.host", "localhost");
        String port = AppConfig.get("db.mysql.port", "3306");
        String database = AppConfig.get("db.mysql.database", "facial_attendance");
        String params = AppConfig.get("db.mysql.params",
            "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");

        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, database, params);

        config.setPoolName("IceFX-MySQL");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(AppConfig.get("db.mysql.username", "root"));
        config.setPassword(AppConfig.get("db.mysql.password", ""));
        config.setDriverClassName(MYSQL_DRIVER);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }

    private static void configureSQLite(HikariConfig config) throws IOException {
        String configuredPath = AppConfig.get("db.sqlite.path", "data/facial_attendance.db");
        Path sqlitePath = Paths.get(configuredPath).toAbsolutePath();
        if (sqlitePath.getParent() != null) {
            Files.createDirectories(sqlitePath.getParent());
        }

        config.setPoolName("IceFX-SQLite");
        config.setJdbcUrl("jdbc:sqlite:" + sqlitePath);
        config.setDriverClassName(SQLITE_DRIVER);
        config.setConnectionTestQuery("SELECT 1");
    }

    private static void applyPoolSettings(HikariConfig config, boolean sqlite) {
        int defaultMax = sqlite ? 5 : 10;
        int defaultMin = sqlite ? 1 : 2;

        int maxPool = AppConfig.getInt("db.pool.maxSize", defaultMax);
        int minIdle = AppConfig.getInt("db.pool.minIdle", defaultMin);

        if (sqlite) {
            maxPool = Math.min(maxPool, defaultMax);
            minIdle = Math.min(minIdle, maxPool);
        }

        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(getLong("db.pool.connectionTimeout", 30_000L));
        config.setIdleTimeout(getLong("db.pool.idleTimeout", 600_000L));
        config.setMaxLifetime(getLong("db.pool.maxLifetime", 1_800_000L));
    }

    private static long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(AppConfig.get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid numeric value for {} - using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Obtain a connection from the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }
        return dataSource.getConnection();
    }

    /**
     * Close the pool (invoked during application shutdown).
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    /**
     * Simple connectivity check.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }

    /**
     * Expose basic pool usage statistics.
     */
    public static String getPoolStats() {
        if (dataSource != null) {
            return String.format("Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "Pool not initialized";
    }
}
