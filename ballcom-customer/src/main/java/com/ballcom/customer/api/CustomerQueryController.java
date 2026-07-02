package com.ballcom.customer.api;

import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.customer.api.dto.CustomerViewResponse;

@RestController
@RequestMapping("/api/customer")
public class CustomerQueryController {
    private final JdbcTemplate jdbcTemplate;

    public CustomerQueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerViewResponse> getCustomerById(@PathVariable UUID customerId) {
        String sql = "SELECT customer_id, name, email, street, house_number, city, zip_code FROM customer_views WHERE customer_id = ?";

        try {
            CustomerViewResponse customerView = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
            new CustomerViewResponse(
                UUID.fromString(rs.getString("customer_id")),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("street"),
                rs.getString("house_number"),
                rs.getString("city"),
                rs.getString("zip_code")
                ),
                customerId

            );
            return ResponseEntity.ok(customerView);
        } catch (EmptyResultDataAccessException e) {
            //als de customer niet bestaat
            return ResponseEntity.notFound().build();
        }
    }
}
