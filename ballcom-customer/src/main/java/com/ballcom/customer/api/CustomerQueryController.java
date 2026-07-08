package com.ballcom.customer.api;

import java.util.List;
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
@RequestMapping("/customers")
public class CustomerQueryController {
    private final JdbcTemplate jdbcTemplate;

    public CustomerQueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping()
    public ResponseEntity<List<CustomerViewResponse>> getCustomers() {
        String sql = "SELECT customer_id, company_name, first_name, last_name, phone_number, street, house_number, city, zip_code FROM customer_views";

        List<CustomerViewResponse> customers = jdbcTemplate.query(sql, (rs, rowNum) ->
            new CustomerViewResponse(
                    UUID.fromString(rs.getString("customer_id")),
                    rs.getString("company_name"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("phone_number"),
                    rs.getString("street"),
                    rs.getString("house_number"),
                    rs.getString("city"),
                    rs.getString("zip_code")
                ));
            return ResponseEntity.ok(customers);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerViewResponse> getCustomerById(@PathVariable UUID customerId) {
        String sql = "SELECT customer_id, company_name, first_name, last_name, phone_number, street, house_number, city, zip_code FROM customer_views WHERE customer_id = ?";

        try {
            CustomerViewResponse customerView = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
            new CustomerViewResponse(
                UUID.fromString(rs.getString("customer_id")),
                rs.getString("company_name"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone_number"),
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
