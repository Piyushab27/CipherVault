package com.ciphervault.model;

public interface Storable extends Searchable {
    /** Header row of the stats CSV used by the DV part. No password column on purpose. */
    String CSV_HEADER = "category,length,strength,has_symbol,is_reused,age_days";

    String toCsv();
}
