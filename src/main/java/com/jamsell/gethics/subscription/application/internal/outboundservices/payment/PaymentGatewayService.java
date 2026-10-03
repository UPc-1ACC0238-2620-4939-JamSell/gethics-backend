package com.jamsell.gethics.subscription.application.internal.outboundservices.payment;

import java.math.BigDecimal;

public interface PaymentGatewayService {

    PaymentResult charge(PaymentRequest request);

    record PaymentRequest(BigDecimal amount, String currency, String paymentToken, String description) {
    }

    record PaymentResult(boolean approved, String reference, String failureReason) {

        public static PaymentResult approved(String reference) {
            return new PaymentResult(true, reference, null);
        }

        public static PaymentResult rejected(String failureReason) {
            return new PaymentResult(false, null, failureReason);
        }
    }
}
