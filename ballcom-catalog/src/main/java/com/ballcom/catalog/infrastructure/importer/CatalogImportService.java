package com.ballcom.catalog.infrastructure.importer;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class CatalogImportService {

    private JdbcTemplate jdbcTemplate;

    public CatalogImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void importNightlyCatalogs() {
        System.out.println("START: Nachtelijke import van catalogus gestart...");

        try (InputStream inputStream = new ClassPathResource("fake_catalog_data_export.csv").getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build())) {

                String sql = "INSERT INTO catalog_views (catalog_id,item_name, price, description, category, availability, owner, updated_at)"+
                """
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (catalog_id) DO UPDATE
                SET item_name = EXCLUDED.item_name,
                price = EXCLUDED.price,
                description = EXCLUDED.description,
                category = EXCLUDED.category,
                availability = EXCLUDED.availability,
                owner = EXCLUDED.owner,
                updated_at = EXCLUDED.updated_at
                """;

                int counter = 0;
                for(CSVRecord record : csvParser) {
                    try {
                        String itemName = record.get("Item Name");
                        String price = record.get("Price");
                        String description = record.get("Description");
                        String category = record.get("Category");
                        String availability = record.get("Availability");
                        String owner = record.get("owner");

                        UUID catalogId = UUID.randomUUID();

                        jdbcTemplate.update(
                            sql,
                            catalogId,
                            itemName,
                            price,
                            description,
                            category,
                            availability,
                            owner,
                            Timestamp.from(Instant.now())
                        );
                        counter++;
                    } catch (Exception e) {
                        System.out.println("Fout bij verwerken van CSV-regel " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }

                System.out.println("SUCCESS: Import afgerond. " + counter + " catalogus verwerkt/geüpdatet.");
    
            } catch (Exception e) {
                System.err.println("Grote fout tijdens de import-verwerking: " + e.getMessage());
            }
        }
}
