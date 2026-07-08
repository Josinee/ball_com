package com.ballcom.catalog.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.catalog.infrastructure.importer.CatalogImportService;

@RestController
@RequestMapping("/catalogs")
public class CatalogImportController {
    private final CatalogImportService catalogImportService;

    public CatalogImportController(CatalogImportService catalogImportService) {
        this.catalogImportService = catalogImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<String> triggerImport() {
        try {
            System.out.println("Handmatige trigger ontvangen via REST API!");

            catalogImportService.importNightlyCatalogs();
            
            return ResponseEntity.ok("Import handmatig succesvol uitgevoerd! Check de console voor details.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Er ging iets mis tijdens de handmatige import: " + e.getMessage());
        }
    }
    
}
