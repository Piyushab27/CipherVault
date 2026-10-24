package com.ciphervault.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC steps: load the driver, get a connection, run the query, read the results, close.
 * Connection settings come from CV_DB_URL, CV_DB_USER and CV_DB_PASS (defaults: local MySQL, root, no password).
 */
public final class DBConnection {
    private static final String URL = env("CV_DB_URL",
            "jdbc:mysql://localhost:3306/ciphervault?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true");
    private static final String USER = env("CV_DB_USER", "root");
    private static final String PASSWORD = env("CV_DB_PASS", "");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Type 4 driver, MySQL Connector/J
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL Connector/J is not on the CLASSPATH");
        }
    }

    private DBConnection() {
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? fallback : value;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Creates the database, tables, view, trigger and procedure on first run. */
    public static void initialise() throws SQLException, IOException {
        List<String> statements;
        try (InputStream in = DBConnection.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql is missing from the jar");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            statements = parseScript(script);
        }
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            for (String sql : statements) {
                if (sql.regionMatches(true, 0, "USE ", 0, 4)) {
                    continue; // the URL already selects the database
                }
                st.execute(sql);
            }
            addMissingUserColumns(con, st);
        }
    }

    /** Databases created by an older version lack the security-question columns; add them. */
    private static void addMissingUserColumns(Connection con, Statement st) throws SQLException {
        String[][] columns = {
            {"security_question", "VARCHAR(150)"},
            {"security_answer_hash", "VARCHAR(64)"},
            {"reset_attempts", "INT DEFAULT 0"}
        };
        for (String[] column : columns) {
            try (ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), null, "users", column[0])) {
                if (!rs.next()) {
                    st.execute("ALTER TABLE users ADD COLUMN " + column[0] + " " + column[1]);
                }
            }
        }
    }

    /** Splits a mysql-client style script into statements. Understands -- comments and DELIMITER. */
    public static List<String> parseScript(String script) throws IOException {
        List<String> statements = new ArrayList<>();
        String delimiter = ";";
        StringBuilder current = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(script))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty() && current.length() == 0) {
                    continue;
                }
                if (trimmed.regionMatches(true, 0, "DELIMITER ", 0, 10)) {
                    delimiter = trimmed.substring(10).trim();
                    continue;
                }
                current.append(line).append('\n');
                String soFar = current.toString().trim();
                if (soFar.endsWith(delimiter)) {
                    statements.add(soFar.substring(0, soFar.length() - delimiter.length()).trim());
                    current.setLength(0);
                }
            }
        }
        return statements;
    }
}
