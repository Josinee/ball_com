package com.ballcom.catalog.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.catalog.api.dto.CatalogCreateRequest;
import com.ballcom.catalog.api.dto.CatalogCreatedResponse;
import com.ballcom.catalog.application.CatalogCommandHandler;
import com.ballcom.catalog.application.commands.CreateCatalogCommand;


@RestController
@RequestMapping("/api/catalogs")
public class CatalogCommandController {
    private final CatalogCommandHandler commandHandler;

    public CatalogCommandController(CatalogCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping
    public ResponseEntity<CatalogCreatedResponse> createCatalog(@RequestBody CatalogCreateRequest request) {

        var command = new CreateCatalogCommand(
                request.itemName(),
                request.price(),
                request.description(),
                request.category(),
                request.availability()
        );
        
        UUID catalogId = commandHandler.handle(command);
        return ResponseEntity.accepted().body(new CatalogCreatedResponse(catalogId));
    }
    
}
