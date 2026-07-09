package com.ballcom.customer.infrastructure.importer;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomerImportService {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public CustomerImportService(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void importNightlyCustomers() {
        System.out.println("START: Nachtelijke import van klantgegevens gestart...");

        try (InputStream inputStream = new ClassPathResource("fake_customer_data_export.csv").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            String sql = """
                INSERT INTO customer_views (customer_id, company_name, first_name, last_name, phone_number, street, house_number, city, zip_code, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (customer_id) DO UPDATE
                SET company_name = EXCLUDED.company_name,
                    first_name = EXCLUDED.first_name,
                    last_name = EXCLUDED.last_name,
                    phone_number = EXCLUDED.phone_number,
                    street = EXCLUDED.street,
                    house_number = EXCLUDED.house_number,
                    city = EXCLUDED.city,
                    zip_code = EXCLUDED.zip_code,
                    updated_at = EXCLUDED.updated_at
            """;

            int counter = 0;
            for (CSVRecord record : csvParser) {
                try {
                    String companyName = record.get("Company Name");
                    String firstName = record.get("First Name");
                    String lastName = record.get("Last Name");
                    String phoneNumber = record.get("Phone Number");
                    String fullAddress = record.get("Address");

                    String uniqueKey = (firstName + "|" + lastName + "|" + (phoneNumber != null ? phoneNumber : "")).toLowerCase().replaceAll("\\s+", "");
                    UUID customerId = UUID.nameUUIDFromBytes(uniqueKey.getBytes());

                    String street = "";
                    String houseNumber = "";
                    String zipCode = "";
                    String city = "";

                    if (fullAddress != null && fullAddress.contains(",")) {
                        String[] parts = fullAddress.split(",", 2);
                        String streetAndNr = parts[0].trim();
                        String zipAndCity = parts[1].trim();

                        int lastSpaceStreet = streetAndNr.lastIndexOf(" ");
                        if (lastSpaceStreet > 0) {
                            street = streetAndNr.substring(0, lastSpaceStreet).trim();
                            houseNumber = streetAndNr.substring(lastSpaceStreet).trim();
                        }

                        int firstSpaceZip = zipAndCity.indexOf(" ");
                        if (firstSpaceZip > 0) {
                            zipCode = zipAndCity.substring(0, firstSpaceZip).trim();
                            city = zipAndCity.substring(firstSpaceZip).trim();
                        }
                    }

                    Instant occurredAt = Instant.now();

                    jdbcTemplate.update(
                            sql,
                            customerId,
                            companyName,
                            firstName,
                            lastName,
                            phoneNumber,
                            street,
                            houseNumber,
                            city,
                            zipCode,
                            Timestamp.from(occurredAt)
                    );

                    publishCustomerRegisteredEvent(
                            customerId,
                            companyName,
                            firstName,
                            lastName,
                            occurredAt
                    );

                    counter++;
                } catch (Exception e) {
                    System.err.println("Fout bij verwerken van CSV-regel " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }

            System.out.println("SUCCES: Import afgerond. " + counter + " klanten verwerkt/geüpdatet.");

        } catch (Exception e) {
            System.err.println("Grote fout tijdens de import-verwerking: " + e.getMessage());
        }
    }

    private void publishCustomerRegisteredEvent(
            UUID customerId,
            String companyName,
            String firstName,
            String lastName,
            Instant occurredAt
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("companyName", companyName);
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);

        GenericDomainEvent event = new GenericDomainEvent(
                UUID.randomUUID(),
                customerId,
                0,
                EventType.CUSTOMER_REGISTERED,
                occurredAt,
                payload
        );

        rabbitTemplate.convertAndSend(
                "customer.exchange",
                "customer.registered",
                event
        );

        System.out.println("CUSTOMER IMPORT: CUSTOMER_REGISTERED event gepubliceerd voor " + customerId);
    }
}