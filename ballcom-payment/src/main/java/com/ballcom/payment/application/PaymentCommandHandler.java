package com.ballcom.payment.application;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ballcom.payment.domain.PaymentAggregate;
import com.ballcom.payment.domain.PaymentMethod;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentCommandHandler {
    private final EventStore eventStore;
    private JdbcTemplate jdbcTemplate;

    public PaymentCommandHandler(EventStore eventStore, JdbcTemplate jdbcTemplate) {
        this.eventStore = eventStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * PHASE 1: Het registreren van de betalingsintentie.
     * Dit wordt direct aangeroepen zodra de Order is geplaatst.
     */
    @Transactional
    public void handle(ProcessPaymentCommand command) {
        // Valideer basis business rules vóórdat we events aanmaken
        if (command.total().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Het totale bedrag moet groter zijn dan 0");
        }

        // 1. Initialiseer de betaling (Status wordt INITIATED)
        // We gebruiken command.orderId() als de unieke sleutel (Aggregate ID)
        PaymentAggregate payment = PaymentAggregate.initiate(
            command.paymentId(),
            command.orderId(), 
            command.customerId(), 
            command.total(), 
            command.paymentMethod()
        );
        
        // 2. Pas de business rules toe op basis van de methode
        if (command.paymentMethod().equals(PaymentMethod.AFTERPAY)) {
            // Achteraf betalen? Direct in de wachtstand zetten voor bezorging!
            payment.holdForDelivery(); 
        } 
        // LET OP: Als het PREPAY is, doen we hier niks! De status blijft INITIATED 
        // totdat de klant daadwerkelijk via de bank heeft betaald.

        // 3. Sla de wijzigingen op
        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }

    /**
     * PHASE 2A: De betaling is geslaagd!
     * Dit wordt getriggerd door een webhook van je Payment Provider (Adyen/Mollie)
     * of wanneer een AFTERPAY order succesvol is bezorgd en de factuur is voldaan.
     */
    @Transactional
    public UUID handle(CompletePaymentCommand command) {
        // 1. Zoek de actieve betaling op basis van de ORDER ID uit het command
        String lookupSql = "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?";
        UUID paymentId;
        try {
            paymentId = jdbcTemplate.queryForObject(lookupSql, UUID.class, command.orderId());
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new RuntimeException("Kan betaling niet afronden: Geen actieve betaling gevonden voor orderId: " + command.orderId());
        }
        
        // 2. Laad de historie uit de event store op basis van de gevonden paymentId
        List<GenericDomainEvent> history = eventStore.loadEvents(paymentId); 
        
        PaymentAggregate payment = new PaymentAggregate();
        payment.loadFromHistory(history);

        payment.complete();

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
        return payment.getId();
    }

    /**
     * PHASE 2B: Het antwoord op jouw vraag: Hoe faalt een betaling?
     * Dit wordt getriggerd als de Payment Provider meldt dat de betaling is mislukt,
     * of als een timer ziet dat de iDEAL-sessie is verlopen.
     */
    @Transactional
    public void handle(FailPaymentCommand command) {
        String lookupSql = "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?";
        UUID paymentId;
        try {
            paymentId = jdbcTemplate.queryForObject(lookupSql, UUID.class, command.orderId());
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Kan betaling niet afronden: Geen actieve betaling gevonden voor orderId: " + command.orderId());
        }

        List<GenericDomainEvent> history = eventStore.loadEvents(paymentId);
        PaymentAggregate payment = new PaymentAggregate();
        
        payment.loadFromHistory(history);

        // 2. Voer business logica uit (Status verandert naar FAILED met een reden)
        payment.fail(command.reason());

        // 3. Sla het PAYMENT_FAILED event op
        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }

    @Transactional
    public void handle(RegisterDeliveryCommand command) {
        String lookupSql = "SELECT payment_id FROM order_payment_mapping WHERE order_id = ?";
        UUID paymentId = jdbcTemplate.queryForObject(lookupSql, UUID.class, command.orderId());
        
        List<GenericDomainEvent> history = eventStore.loadEvents(paymentId); 
        PaymentAggregate payment = new PaymentAggregate();
        payment.loadFromHistory(history);

        // Zet de status op DELIVERY_CONFIRMED
        payment.registerDelivery();

        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }
}