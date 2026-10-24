package com.ciphervault.service;

import com.ciphervault.model.Credential;
import com.ciphervault.model.Storable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class CsvService {
    /** Header of the file that Import CSV reads. */
    public static final String IMPORT_HEADER = "site,username,password,category";

    public record ParseResult(List<Credential> entries, int skipped) {
    }

    private CsvService() {
    }

    /** Writes one stats row per entry. The password itself is never written. */
    public static void exportStats(File file, List<Credential> entries) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            out.write(Storable.CSV_HEADER);
            out.newLine();
            for (Credential c : entries) {
                out.write(c.toCsv());
                out.newLine();
            }
        }
    }

    /** Reads site,username,password[,category] rows. Rows that are too short are skipped. */
    public static ParseResult importEntries(File file) throws IOException {
        List<Credential> entries = new ArrayList<>();
        int skipped = 0;
        try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> f = parseLine(line);
                if (first) {
                    first = false;
                    if (f.get(0).equalsIgnoreCase("site")) {
                        continue; // header row
                    }
                }
                if (f.size() < 3 || f.get(0).isBlank() || f.get(1).isBlank() || f.get(2).isEmpty()) {
                    skipped++;
                    continue;
                }
                String category = f.size() > 3 && !f.get(3).isBlank() ? f.get(3).trim() : "Other";
                entries.add(new Credential(f.get(0).trim(), f.get(1).trim(), f.get(2), category));
            }
        }
        return new ParseResult(entries, skipped);
    }

    /** Splits one CSV line, honouring "double quoted, fields" and "" escapes. */
    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
