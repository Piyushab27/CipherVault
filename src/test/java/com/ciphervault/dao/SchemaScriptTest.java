package com.ciphervault.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaScriptTest {
    @Test
    void schemaSplitsIntoTheExpectedStatements() throws Exception {
        String script;
        try (InputStream in = getClass().getResourceAsStream("/schema.sql")) {
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<String> sql = DBConnection.parseScript(script);
        // database, use, 3 tables, categories insert, view, 2x(drop+create trigger), drop+create procedure = 13
        assertEquals(13, sql.size(), sql.toString());
        String procedure = sql.get(sql.size() - 1);
        assertTrue(procedure.startsWith("CREATE PROCEDURE add_credential"));
        assertTrue(procedure.contains("SET p_id = LAST_INSERT_ID();"), "body keeps its inner semicolons");
        assertTrue(procedure.endsWith("END"));
    }

    @Test
    void delimiterAndCommentsAreHandled() throws Exception {
        List<String> sql = DBConnection.parseScript("-- c\nSELECT 1;\nDELIMITER $$\nBEGIN a; b; END$$\nDELIMITER ;\nSELECT 2;\n");
        assertEquals(List.of("SELECT 1", "BEGIN a; b; END", "SELECT 2"), sql);
    }
}
