package com.ballcom.payment.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ballcom.payment.domain.PaymentAggregate;
import com.ballcom.payment.domain.PaymentMethod;
import com.ballcom.shared.events.GenericDomainEvent;
import com.ballcom.shared.eventsourcing.EventStore;
import java.util.List;

@Component
public class PaymentCommandHandler {
    private final EventStore eventStore;

    public PaymentCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
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
            command.customerId(), 
            command.orderId(), 
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
    public void handle(CompletePaymentCommand command) {
        // 1. Reconstitueer: Haal de geschiedenis op en breng de aggregate tot leven
        List<GenericDomainEvent> history = eventStore.loadEvents(command.orderId());
        PaymentAggregate payment = new PaymentAggregate();
        payment.loadFromHistory(history);

        // 2. Voer business logica uit (Status verandert naar COMPLETED)
        payment.complete();

        // 3. Sla het PAYMENT_COMPLETED event op
        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }

    /**
     * PHASE 2B: Het antwoord op jouw vraag: Hoe faalt een betaling?
     * Dit wordt getriggerd als de Payment Provider meldt dat de betaling is mislukt,
     * of als een timer ziet dat de iDEAL-sessie is verlopen.
     */
    @Transactional
    public void handle(FailPaymentCommand command) {
        // 1. Reconstitueer de betaling uit de Event Store
        List<GenericDomainEvent> history = eventStore.loadEvents(command.orderId());
        PaymentAggregate payment = new PaymentAggregate();
        payment.loadFromHistory(history);

        // 2. Voer business logica uit (Status verandert naar FAILED met een reden)
        payment.fail(command.reason());

        // 3. Sla het PAYMENT_FAILED event op
        eventStore.append(payment.getId(), payment.getUncommitedEvents(), payment.getExpectedVersion());
        payment.clearUncommitedEvents();
    }
}