package com.ballcom.payment.domain;

public enum PaymentStatus implements PaymentState{
    INITIATED {
         @Override
        public void holdForDelivery(PaymentAggregate aggregate) {
            aggregate.emitHoldForDelivery();
        }
        @Override
        public void complete(PaymentAggregate aggregate) {
            aggregate.emitComplete();
        }
        @Override
        public void fail(PaymentAggregate aggregate, String reason) {
            aggregate.emitFail(reason);
        }
    },
       
    PENDING_DELIVERY {
        @Override
        public void complete(PaymentAggregate aggregate) {
            aggregate.emitComplete();
        }
        @Override
        public void fail(PaymentAggregate aggregate, String reason){
            aggregate.emitFail(reason);
        }
    },
    COMPLETED, //er mag niks meer aangepast worden nadat status completed is
    FAILED {
        @Override
        public void complete(PaymentAggregate aggregate) {
            aggregate.emitComplete();
        }
    }
}
