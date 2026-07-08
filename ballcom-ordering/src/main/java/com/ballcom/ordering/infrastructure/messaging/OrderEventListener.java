package com.ballcom.ordering.infrastructure.messaging;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderEventListener {

    private final JdbcTemplate jdbcTemplate;

    public OrderEventListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }
    @Transactional
    @RabbitListener(queues = "ordering-payment-updates-queue")
    public void listenPaymentUpdates(GenericDomainEvent event) {

        System.out.println("[DEBUG] === ONDERDEEL PAYMENT UPDATE GESTART ===");
    System.out.println("[DEBUG] Event ontvangen! Type: " + event.eventType() + " | ID: " + event.eventId());
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if(rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + "al eerder verwerkt"); //TODO engels?
                return;
            }
                Map<String, Object> payload = (Map<String, Object>) event.payload();
                System.out.println("[DEBUG] Volledige payload inhoud: " + payload);
                Object orderIdObj = payload.get("orderId"); 
            if (orderIdObj == null) {
                throw new IllegalArgumentException("Payload mist de cruciale 'orderId' key! Beschikbare keys: " + payload.keySet());
            }
                UUID orderId = UUID.fromString((String) payload.get("orderId"));
            if(EventType.PAYMENT_COMPLETED.equals(event.eventType())) {


                String sql = "UPDATE order_views SET payment_status = 'PAID' WHERE order_id = ?";
                jdbcTemplate.update(sql, orderId);
                System.out.println("READ MODEL: Order " + orderId + " gemarkeerd als PAID.");
            } 

            else if(EventType.PAYMENT_AWAITING_DELIVERY.equals(event.eventType())) {
                //afterpay flow, order is nog niet paid maar betaling is gegarandeerd
                jdbcTemplate.update("UPDATE order_views SET payment_status = 'AFTERPAY_AWAITING_DELIVERY' where order_id = ?", orderId);

            }
            
            else if (EventType.PAYMENT_FAILED.equals(event.eventType())) {

                String sql = "UPDATE order_views SET payment_status = 'PAYMENT_FAILED' WHERE order_id = ?";
                jdbcTemplate.update(sql, orderId);
                System.out.println("READ MODEL: Order " + orderId + " gemarkeerd als PAYMENT FAILED.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Deze methode luistert naar de queue
    @Transactional
    @RabbitListener(queues = "ordering-readmodel-queue")
    public void consume(GenericDomainEvent event) {
        try {
            String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
            int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
            if (rowsAffected == 0) {
                System.out.println("Event " + event.eventId() + " already processed. Skipping.");
                return;
            }
        
            System.out.println("CONSUMER: Event received in read model! Type: " + event.eventType());
            
            if (EventType.ORDER_PLACED.equals(event.eventType())) {
                try {
                    Map<String, Object> payload = (Map<String, Object>) event.payload();

                    UUID orderId = event.aggregateId();
                    UUID customerId = UUID.fromString((String) payload.get("customerId"));
                    BigDecimal totalAmount = new BigDecimal(payload.get("totalAmount").toString());
                    
                    String orderStatus = "PLACED";
                    String paymentStatus = "AWAITING";

                    String sql = """
                        INSERT INTO order_views (order_id, customer_id, total_amount, order_status, payment_status, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (order_id) DO UPDATE 
                        SET order_status = EXCLUDED.order_status, 
                            payment_status = EXCLUDED.payment_status, 
                            updated_at = EXCLUDED.updated_at;
                    """;

                    jdbcTemplate.update(sql, 
                        orderId, 
                        customerId, 
                        totalAmount, 
                        orderStatus,
                        paymentStatus,
                        Timestamp.from(event.occurredAt())
                    );

                    System.out.println("READ MODEL: Order " + orderId + " successfully saved to order_views.");
                } catch (Exception e) {
                    System.err.println("Error processing order event: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (EventType.SHIPMENT_SHIPPED.equals(event.eventType())) {
                try {
                    UUID shipmentId = event.aggregateId();
                    
                    String sqlMapping = "SELECT order_id FROM order_shipment_mapping WHERE shipment_id = ?";
                    UUID orderId = jdbcTemplate.queryForObject(sqlMapping, UUID.class, shipmentId);
                    
                    String sqlUpdate = "UPDATE order_views SET order_status = 'SHIPPED', updated_at = ? WHERE order_id = ?";
                    jdbcTemplate.update(sqlUpdate, Timestamp.from(event.occurredAt()), orderId);
                    
                    System.out.println("READ MODEL: Order " + orderId + " gemarkeerd als SHIPPED.");
                } catch (Exception e) {
                    System.err.println("Error processing shipment shipped event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            

            else if (EventType.SHIPMENT_DELIVERED.equals(event.eventType())) {
                try {
                    UUID shipmentId = event.aggregateId();
                    
                    String sqlMapping = "SELECT order_id FROM order_shipment_mapping WHERE shipment_id = ?";
                    UUID orderId = jdbcTemplate.queryForObject(sqlMapping, UUID.class, shipmentId);
                    
                    String sqlUpdate = "UPDATE order_views SET order_status = 'DELIVERED', updated_at = ? WHERE order_id = ?";
                    jdbcTemplate.update(sqlUpdate, Timestamp.from(event.occurredAt()), orderId);
                    
                    System.out.println("READ MODEL: Order " + orderId + " gemarkeerd als DELIVERED.");
                } catch (Exception e) {
                    System.err.println("Error processing shipment delivered event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in event consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}