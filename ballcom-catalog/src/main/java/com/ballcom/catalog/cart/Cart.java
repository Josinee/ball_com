package com.ballcom.catalog.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ballcom.shared.events.EventType;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.AggregateRoot;

@RestController
@RequestMapping("/catalog/cart")
public class Cart extends AggregateRoot{
    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;

    public record OrderItem(UUID productId, int quantity, BigDecimal unitPrice) {}
    public record AddToCartRequest(UUID productId, int quantity) {}
    public record CartCheckoutCommand(UUID customerId, String paymentMethod, List<OrderItem> items){}
    

    private Map<UUID, List<OrderItem>> carts = new ConcurrentHashMap<>();

    public Cart(RabbitTemplate rabbitTemplate, JdbcTemplate jdbcTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/{customerId}/add")
    public ResponseEntity<String> addToCart(@PathVariable UUID customerId, @RequestBody AddToCartRequest request) {
        try {
            String sql = "SELECT price FROM catalog_views WHERE catalog_id = ?";
            String priceStr = jdbcTemplate.queryForObject(sql, String.class, request.productId());
            
            if (priceStr == null) {
                return ResponseEntity.badRequest().body("Product heeft geen geldige prijs in de catalogus.");
            }
            BigDecimal realUnitPrice = new BigDecimal(priceStr);

            OrderItem item = new OrderItem(request.productId(), request.quantity(), realUnitPrice);

            carts.putIfAbsent(customerId, new ArrayList<>());
            List<OrderItem> cart = carts.get(customerId);
            cart.add(item);

            return ResponseEntity.ok("Product " + item.productId() + " succesvol toegevoegd voor €" + realUnitPrice);

        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ResponseEntity.badRequest().body("FOUT: Product met ID " + request.productId() + " bestaat niet in de catalogus!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Interne fout bij ophalen productprijs: " + e.getMessage());
        }
    }


    @PostMapping("/{customerId}/checkout")
    public ResponseEntity<String> checkoutCart(@PathVariable UUID customerId, @RequestParam String paymentMethod) {
        List<OrderItem> cartitems = carts.get(customerId);
        Cart cart = new Cart(this.rabbitTemplate, this.jdbcTemplate);
        UUID cartId = UUID.randomUUID();
        long nextSequence = cart.getSequenceNumber() + 1;

        BigDecimal total = cartitems.stream().map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> payload = Map.of(
            "customerId", customerId.toString(), 
            "items", cartitems, 
            "paymentMethod", paymentMethod,
            "totalAmount", total.toString()
        );

        
        GenericDomainEvent event = new GenericDomainEvent(UUID.randomUUID(), cartId, nextSequence, EventType.CHECKOUT_REQUESTED, Instant.now(), payload);
        System.out.println(">>> HTTP CHECKOUT HIT! Cart-items: " + cartitems.size());
    rabbitTemplate.convertAndSend("catalog.exchange", "catalog." + event.eventType(), event);
    System.out.println(">>> BERICHT VERSTUURD NAAR RABBITMQ! <<<");
        carts.remove(customerId);
        return ResponseEntity.ok("order is placed");

    }
    @GetMapping("/{customerId}")
    public ResponseEntity<List<OrderItem>> getCart(@PathVariable UUID customerId) {
        List<OrderItem> cartItems = carts.getOrDefault(customerId, new ArrayList<>());
        return ResponseEntity.ok(cartItems);
    }
    

    @Override
    protected void apply(com.ballcom.shared.events.GenericDomainEvent event) {
        this.id = event.aggregateId();
    }
}