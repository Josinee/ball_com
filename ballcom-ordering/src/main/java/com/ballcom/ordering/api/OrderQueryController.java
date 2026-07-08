package com.ballcom.ordering.api;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.ordering.api.dto.OrderViewResponse;


@RestController
@RequestMapping("/orders")
public class OrderQueryController {
    private final JdbcTemplate jdbcTemplate;

    public OrderQueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderViewResponse> getOrderById(@PathVariable UUID orderId) {
        String sql = "SELECT order_id, customer_id, total_amount, order_status, payment_status FROM order_views WHERE order_id = ?";

        try {
            OrderViewResponse orderView = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
            new OrderViewResponse(
                    UUID.fromString(rs.getString("order_id")),
                    UUID.fromString(rs.getString("customer_id")),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("order_status"),
                    rs.getString("payment_status")
                ), 
                orderId
            );
            return ResponseEntity.ok(orderView);
        } catch (EmptyResultDataAccessException e) {
            //als de order niet bestaat
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderViewResponse>> getOrdersByCustomer(@PathVariable UUID customerId) {
        String sql = "SELECT order_id, customer_id, total_amount, order_status, payment_status FROM order_views WHERE customer_id = ?";

        List<OrderViewResponse> orders = jdbcTemplate.query(sql,
        (rs, rowNum) -> new OrderViewResponse(
            rs.getObject("order_id", UUID.class),
            rs.getObject("customer_id", UUID.class),
            rs.getBigDecimal("total_amount"),
            rs.getString("order_status"),
            rs.getString("payment_status")
        ),customerId);
        return ResponseEntity.ok(orders);
    }
    
}
