package com.ballcom.catalog.api;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.catalog.api.dto.CatalogViewResponse;

@RestController
@RequestMapping("/api/catalog")
public class CatalogQueryController {
    private final JdbcTemplate jdbcTemplate;

    public CatalogQueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //alle item van catalog ophalen
    @GetMapping
    public ResponseEntity<List<CatalogViewResponse>> getAllCatalogItems() {
        String sql = """
            SELECT catalog_id, item_name, price, description, category, availability, owner
            FROM catalog_views
            ORDER BY item_name
                """;

                List<CatalogViewResponse> catalogItems = jdbcTemplate.query(sql, (rs, rowNum) ->
                new CatalogViewResponse(
                UUID.fromString(rs.getString("catalog_id")),
                rs.getString("item_name"),
                rs.getString("price"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("availability"),
                rs.getString("owner")
            )
        );

        return ResponseEntity.ok(catalogItems);
    }

    //1 item ophalen
    @GetMapping("/{catalogId}")
    public ResponseEntity<CatalogViewResponse> getCatalogById(@PathVariable UUID catalogId) {
        String sql = "SELECT catalog_id, item_name, price, description, category, availability, owner FROM catalog_views WHERE catalog_id = ?";

        try {
            CatalogViewResponse catalogView = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
            new CatalogViewResponse(
                UUID.fromString(rs.getString("catalog_id")),
                rs.getString("item_name"),
                rs.getString("price"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("availability"),
                rs.getString("owner")
                ),
                catalogId

            );
            return ResponseEntity.ok(catalogView);
        } catch (EmptyResultDataAccessException e) {
            //als de catalog niet bestaat
            return ResponseEntity.notFound().build();
        }
    }
    
}
