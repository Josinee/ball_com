package com.ballcom.payment.infrastructure.messaging;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ballcom.payment.application.PaymentCommandHandler;
import com.ballcom.payment.application.commands.ProcessPaymentCommand;
import com.ballcom.payment.application.commands.RegisterDeliveryCommand;
import com.ballcom.payment.domain.PaymentMethod;
import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent; 

@Component
public class PaymentEventListener {
    private final JdbcTemplate jdbcTemplate;
    private final PaymentCommandHandler commandHandler;

    public PaymentEventListener(JdbcTemplate jdbcTemplate, PaymentCommandHandler commandHandler) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandHandler = commandHandler;
    }

    @RabbitListener(queues = "payment-order-placed-queue")
    @Transactional
    public void consume(GenericDomainEvent event) {
        System.out.println("=== CONSUMER ONTVANGEN ===");
        System.out.println("RABBITMQ: Bericht ontvangen uit payment-order-placed-queue. Event ID: " + event.eventId() + ", Type: " + event.eventType());

        String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
        

        if (rowsAffected == 0) {
            System.out.println("Event " + event.eventId() + "al eerder verwerkt");
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) event.payload();

        // als er een order geplaatst wordt, worden de gegevens in de database payment_views opgeslagen
        if (EventType.ORDER_PLACED.equals(event.eventType())) {
            UUID orderId = event.aggregateId();
            String methodFromEvent = (String) payload.get("paymentMethod");
            PaymentMethod mappedMethod = PaymentMethod.valueOf(methodFromEvent.toUpperCase());
            BigDecimal productAmount = new BigDecimal(payload.get("totalAmount").toString());

            UUID tempPaymentId = UUID.randomUUID();
            String mappingSql = """
                INSERT INTO order_payment_mapping (order_id, payment_id)
                VALUES (?, ?) ON CONFLICT (order_id) DO NOTHING
            """;


            jdbcTemplate.update(mappingSql, orderId, tempPaymentId);
            UUID actualPaymentId = jdbcTemplate.queryForObject("SELECT payment_id FROM order_payment_mapping WHERE order_id = ?", UUID.class, orderId);

            String sql = """
            INSERT INTO payment_views (payment_id, order_id, customer_id, total_amount, payment_method, status, updated_at)
            VALUES (?, ?, ?, ?, ?, 'INITIATED', ?)
            ON CONFLICT (payment_id) DO UPDATE 
            SET customer_id = EXCLUDED.customer_id,
                payment_method = EXCLUDED.payment_method,
                total_amount = payment_views.total_amount + EXCLUDED.total_amount,
                updated_at = EXCLUDED.updated_at;
            """;

            jdbcTemplate.update(sql, actualPaymentId, orderId, UUID.fromString((String) payload.get("customerId")), productAmount, mappedMethod.name(), Timestamp.from(event.occurredAt()));
            System.out.println("PAYMENT VIEW: Productgegevens verwerkt voor payment: " + actualPaymentId);

        } else if (EventType.COSTS_CALCULATED.equals(event.eventType())) {
            
            UUID orderId = UUID.fromString((String) payload.get("orderId"));
            BigDecimal shippingPrice = new BigDecimal(payload.get("shippingPrice").toString());
            // krijg prijs van totale producten, voeg shipping cost erbij en update de status naar INITIATED
            //als order_placed nog niet in de database staat, voegt hij een nieuwe rij toe met alleen de totale amount
            UUID tempPaymentId = UUID.randomUUID();
            String mappingSql = "INSERT INTO order_payment_mapping (order_id, payment_id) VALUES (?, ?) ON CONFLICT (order_id) DO NOTHING";
            jdbcTemplate.update(mappingSql, orderId, tempPaymentId);

            UUID actualPaymentId = jdbcTemplate.queryForObject("SELECT payment_id FROM order_payment_mapping WHERE order_id = ?", UUID.class, orderId);

            // 2. UPSERT in payment_views op basis van payment_id
            String upsertSql = """
                INSERT INTO payment_views (payment_id, order_id, customer_id, total_amount, payment_method, status, updated_at)
                VALUES (?, ?, '00000000-0000-0000-0000-000000000000', ?, 'UNKNOWN', 'INITIATED', ?)
                ON CONFLICT (payment_id) DO UPDATE 
                SET total_amount = payment_views.total_amount + EXCLUDED.total_amount,
                updated_at = EXCLUDED.updated_at;
            """;
            jdbcTemplate.update(upsertSql, actualPaymentId, orderId, shippingPrice, Timestamp.from(event.occurredAt()));

            // 3. Gegevens ophalen voor het Command
            String selectSql = "SELECT total_amount, customer_id, payment_method FROM payment_views WHERE payment_id = ?";
            Map<String, Object> updatedRow = jdbcTemplate.queryForMap(selectSql, actualPaymentId);

            // SAGA Check: Controleer op de gecorrigeerde volledige nul-UUID string
            if ("00000000-0000-0000-0000-000000000000".equals(updatedRow.get("customer_id").toString())) {
                System.out.println("PAYMENT: Verzendkosten opgeslagen onder paymentId " + actualPaymentId + ", wacht op ORDER_PLACED...");
                return;
            }

            UUID customerId = (UUID) updatedRow.get("customer_id");
            BigDecimal finalTotal = (BigDecimal) updatedRow.get("total_amount");

            
            var command = new ProcessPaymentCommand(
                actualPaymentId,
                customerId,
                orderId,
                finalTotal,
                PaymentMethod.valueOf(((String) updatedRow.get("payment_method")).toUpperCase())
            );

            commandHandler.handle(command);
            System.out.println("PAYMENT: Verzendkosten (€" + shippingPrice + ") toegevoegd! Totaalbedrag is nu: €" + finalTotal);
        }
        

    }

    @RabbitListener(queues = "payment-shipment-delivery-queue")
    @Transactional
    public void consumeDelivery(GenericDomainEvent event) {
        // String match voorkomt Enum-deserialisatie fouten
        if (!"ORDER_DELIVERED".equals(event.eventType().toString())) {
            return;
        }

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        UUID orderId = UUID.fromString((String) payload.get("orderId"));

        System.out.println("PAYMENT: Pakket is bezorgd! Status in Payment wordt verzet zodat klant straks /pay kan doen.");
        
        // 1. Voer de state transition uit in de aggregate
        commandHandler.handle(new RegisterDeliveryCommand(orderId));

        // 2. Update direct je read model zodat het klopt in je database GUI
        String updateSql = "UPDATE payment_views SET status = 'DELIVERY_CONFIRMED', updated_at = ? WHERE order_id = ?";
        jdbcTemplate.update(updateSql, Timestamp.from(event.occurredAt()), orderId);
    }

    @RabbitListener(queues = "payment-readmodel-queue")
    @Transactional
    public void consumePaymentLifecycleEvents(GenericDomainEvent event) {
        String idempotencySql = "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int rowsAffected = jdbcTemplate.update(idempotencySql, event.eventId(), Timestamp.from(event.occurredAt()));
        if (rowsAffected == 0) return;

        Map<String, Object> payload = (Map<String, Object>) event.payload();
        UUID paymentId = event.aggregateId(); // Dit is de ID vanuit je PaymentAggregate

        if (EventType.PAYMENT_INITIATED.equals(event.eventType())) {
            UUID orderId = UUID.fromString((String) payload.get("orderId"));
            
            // Sla de officiële aggregate paymentId op in de mapping tabel (overschrijf de tijdelijke mock UUID)
            String mappingSql = "INSERT INTO order_payment_mapping (order_id, payment_id) VALUES (?, ?) ON CONFLICT (order_id) DO UPDATE SET payment_id = EXCLUDED.payment_id";
            jdbcTemplate.update(mappingSql, orderId, paymentId);

            // Update de payment_views tabel zodat de payment_id nu klopt met de event store
            String sql = "UPDATE payment_views SET payment_id = ?, status = 'INITIATED', updated_at = ? WHERE order_id = ?";
            jdbcTemplate.update(sql, paymentId, Timestamp.from(event.occurredAt()), orderId);
            System.out.println("READ MODEL: Payment " + paymentId + " gekoppeld aan Order " + orderId);
        }

        
        else if (EventType.PAYMENT_COMPLETED.equals(event.eventType())) {
            // ONDERDEEL VAN DE FIX: We zoeken nu direct op payment_id! Dat matcht 1-op-1 met het event.
            String sql = "UPDATE payment_views SET status = 'COMPLETED', updated_at = ? WHERE payment_id = ?";
            int updated = jdbcTemplate.update(sql, Timestamp.from(event.occurredAt()), paymentId);
            System.out.println("READ MODEL: Payment " + paymentId + " staat op COMPLETED (Rijen geraakt: " + updated + ")");
        } 

        else if (EventType.PAYMENT_AWAITING_DELIVERY.equals(event.eventType())) {
            String sql = "UPDATE payment_views set status = 'AWAITING_DELIVERY', updated_at = ? WHERE payment_id = ?";
            jdbcTemplate.update(sql, Timestamp.from(event.occurredAt()), paymentId);
            System.out.println("READ MODEL: Payment " + paymentId + " staat op AWAITING_DELIVERY ");
        }
        
        else if (EventType.PAYMENT_FAILED.equals(event.eventType())) {
            // ONDERDEEL VAN DE FIX: Zoeken op payment_id
            String sql = "UPDATE payment_views SET status = 'FAILED', updated_at = ? WHERE payment_id = ?";
            jdbcTemplate.update(sql, Timestamp.from(event.occurredAt()), paymentId);
            System.out.println("READ MODEL: Payment " + paymentId + " is GEFAALD");
        }
    }


}
