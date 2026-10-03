package com.jamsell.gethics.subscription.infrastructure.paymentgateway;

import com.jamsell.gethics.subscription.application.internal.outboundservices.payment.PaymentGatewayService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SandboxPaymentGatewayService implements PaymentGatewayService {

    public static final String APPROVED_TOKEN = "tok_approved";
    public static final String DECLINED_TOKEN = "tok_declined";
    public static final String INSUFFICIENT_FUNDS_TOKEN = "tok_insufficient_funds";

    @Override
    public PaymentResult charge(PaymentRequest request) {
        return switch (request.paymentToken()) {
            case APPROVED_TOKEN -> PaymentResult.approved("sbx_" + UUID.randomUUID());
            case DECLINED_TOKEN -> PaymentResult.rejected("La tarjeta fue rechazada");
            case INSUFFICIENT_FUNDS_TOKEN -> PaymentResult.rejected("Fondos insuficientes");
            default -> PaymentResult.rejected("El método de pago no es válido");
        };
    }
}
