package com.ballcom.shared.messaging;

import com.ballcom.shared.events.GenericDomainEvent;

public interface EventPublisher {
    void publish(GenericDomainEvent event);
    
}
