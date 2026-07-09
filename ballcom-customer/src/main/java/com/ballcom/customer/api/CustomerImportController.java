package com.ballcom.customer.api;

import com.ballcom.customer.infrastructure.importer.CustomerImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


// Controller alleen om de customer nightly import aan te roepen wanneer je wilt om te testen


@RestController
@RequestMapping("/customers")
public class CustomerImportController {

    private final CustomerImportService customerImportService;

    public CustomerImportController(CustomerImportService customerImportService) {
        this.customerImportService = customerImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<String> triggerImport() {
        try {
            customerImportService.importNightlyCustomers();
            
            return ResponseEntity.ok("Import handmatig succesvol uitgevoerd! Check de console voor details.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Er ging iets mis tijdens de handmatige import: " + e.getMessage());
        }
    }
}