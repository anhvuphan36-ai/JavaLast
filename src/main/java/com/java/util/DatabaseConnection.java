package com.java.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("[DB] Không tìm thấy file config.properties.");
                System.err.println("[DB] Vui lòng copy src/main/resources/config.properties.example thành src/main/resources/config.properties và điền thông tin DB.");
                return;
            }
            props.load(input);
        } catch (IOException e) {
            System.err.println("[DB] Lỗi đọc config.properties: " + e.getMessage());
            return;
        }

        String url = ensureUtf8Params(props.getProperty("db.url", ""));
        String username = props.getProperty("db.username", "");
        String password = props.getProperty("db.password", "");
        String driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");

        if (url.isEmpty() || username.isEmpty()) {
            System.err.println("[DB] Thiếu cấu hình db.url hoặc db.username trong config.properties.");
            System.err.println("[DB] Vui lòng copy src/main/resources/config.properties.example thành src/main/resources/config.properties và điền thông tin.");
            return;
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Không tìm thấy driver MySQL: " + e.getMessage());
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("JudgeSystemHikariPool");

        dataSource = new HikariDataSource(config);
        initialized = true;
        System.out.println("[DB] Connection pool initialized: " + url);
    }

    private static String ensureUtf8Params(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        if (url.contains("characterEncoding")) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfig();
        }
        if (dataSource == null) {
            throw new SQLException("Database connection pool not initialized. Check config.properties.");
        }
        return dataSource.getConnection();
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("[DB] Kết nối CSDL thành công!");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Kết nối CSDL thất bại: " + e.getMessage());
        }
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DB] Connection pool closed.");
        }
    }
}
