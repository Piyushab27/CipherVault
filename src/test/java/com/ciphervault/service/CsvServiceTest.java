package com.ciphervault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ciphervault.model.Credential;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvServiceTest {
    @TempDir
    Path dir;

    @Test
    void exportHasTheStatsColumnsAndNoPasswords() throws Exception {
        Credential c = new Credential("Gmail", "me", "TopSecret#1", "College");
        c.setStrength("Strong");
        File file = dir.resolve("vault_stats.csv").toFile();
        CsvService.exportStats(file, List.of(c));
        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals("category,length,strength,has_symbol,is_reused,age_days", lines.get(0));
        assertEquals(2, lines.size());
        assertEquals("College,11,Strong,true,false,0", lines.get(1));
        assertFalse(String.join("\n", lines).contains("TopSecret"));
    }

    @Test
    void importReadsRowsHandlesQuotesAndSkipsBadLines() throws Exception {
        Path csv = dir.resolve("entries.csv");
        Files.writeString(csv, String.join("\n",
                "site,username,password,category",
                "Instagram,poojitha_07,Insta#2026x,Social",
                "\"Shop, Online\",me,\"pa\"\"ss,1\",Shopping",
                "NoPassword,me,",
                "",
                "Plain,me,pw123"));
        CsvService.ParseResult result = CsvService.importEntries(csv.toFile());
        assertEquals(3, result.entries().size());
        assertEquals(1, result.skipped());
        assertEquals("Shop, Online", result.entries().get(1).getSiteName());
        assertEquals("pa\"ss,1", result.entries().get(1).getPassword());
        assertEquals("Other", result.entries().get(2).getCategory());
    }
}
