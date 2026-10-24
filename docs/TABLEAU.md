# Tableau Public steps (DV weeks 6 to 11)

1. Export your real data from the app: **File > Export CSV**, or start with `data/sample_vault_stats.csv`.
2. Tableau Public > Connect > Text file > pick the CSV.
3. Dimensions: category, strength, has_symbol, is_reused. Measures: length, age_days.
4. Calculated field **% strong**: `SUM(IF [strength] = "Strong" THEN 1 ELSE 0 END) / COUNT([strength])`
   (format as percentage). Make **% weak** the same way with "Weak".
5. Sheets: entries per category (bar), strength by category (stacked bar, strength on Color), length by strength (box plot),
   reuse (pie of is_reused).
6. Add a category filter (Show Filter) and a reference line on % weak for the target.
7. Final dashboard: drag the sheets onto one dashboard titled "How many passwords are weak, reused or old, by category".
   Use `age_days > 180` as a calculated field **old** for the old-password count.
8. File > Save to Tableau Public.
