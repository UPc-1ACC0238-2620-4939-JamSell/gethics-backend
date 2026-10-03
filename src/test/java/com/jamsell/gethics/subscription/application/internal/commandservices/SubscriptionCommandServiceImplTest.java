package com.jamsell.gethics.subscription.application.internal.commandservices;

import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.application.result.Result;
import com.jamsell.gethics.subscription.application.internal.outboundservices.payment.PaymentGatewayService;
import com.jamsell.gethics.subscription.application.internal.outboundservices.payment.PaymentGatewayService.PaymentResult;
import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import com.jamsell.gethics.subscription.domain.model.aggregates.Subscription;
import com.jamsell.gethics.subscription.domain.model.commands.SubscribeToPlanCommand;
import com.jamsell.gethics.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCommandServiceImplTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    private SubscriptionCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionCommandServiceImpl(planRepository, subscriptionRepository, paymentGatewayService);
    }

    @Test
    void subscribe_withUnknownPlan_returnsNotFound() {
        when(planRepository.findByCode("INEXISTENTE")).thenReturn(Optional.empty());

        var result = service.handle(new SubscribeToPlanCommand(1L, "inexistente", "tok_approved"));

        assertThat(errorOf(result).code()).isEqualTo("PLAN_NOT_FOUND");
    }

    @Test
    void subscribe_toFreePlan_isRejectedByBusinessRule() {
        var free = plan(1L, "GRATUITO", "0.00");
        when(planRepository.findByCode("GRATUITO")).thenReturn(Optional.of(free));

        var result = service.handle(new SubscribeToPlanCommand(1L, "GRATUITO", "tok_approved"));

        assertThat(errorOf(result).code()).isEqualTo("BUSINESS_RULE_VIOLATION");
        verify(paymentGatewayService, never()).charge(any());
    }

    @Test
    void subscribe_withRejectedPayment_keepsFreePlanAndSavesNothing() {
        var basic = plan(2L, "BASICO", "19.90");
        when(planRepository.findByCode("BASICO")).thenReturn(Optional.of(basic));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        when(paymentGatewayService.charge(any())).thenReturn(PaymentResult.rejected("La tarjeta fue rechazada"));

        var result = service.handle(new SubscribeToPlanCommand(1L, "BASICO", "tok_declined"));

        assertThat(errorOf(result).code()).isEqualTo("PAYMENT_REJECTED");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_withApprovedPayment_activatesPlanAndCancelsPreviousOne() {
        var premium = plan(3L, "PREMIUM", "49.90");
        var previous = new Subscription(1L, 2L, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600), "sbx_old");
        when(planRepository.findByCode("PREMIUM")).thenReturn(Optional.of(premium));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(List.of(previous));
        when(paymentGatewayService.charge(any())).thenReturn(PaymentResult.approved("sbx_new"));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new SubscribeToPlanCommand(1L, "PREMIUM", "tok_approved"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(previous.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        var current = result.toOptional().orElseThrow();
        assertThat(current.plan().getCode()).isEqualTo("PREMIUM");
        assertThat(current.subscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(current.subscription().getPaymentReference()).isEqualTo("sbx_new");
        assertThat(current.subscription().getEndsAt()).isAfter(Instant.now());
    }

    @Test
    void subscribe_toSamePlanAlreadyActive_returnsConflict() {
        var basic = plan(2L, "BASICO", "19.90");
        var active = new Subscription(1L, 2L, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), "sbx_old");
        when(planRepository.findByCode("BASICO")).thenReturn(Optional.of(basic));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(List.of(active));

        var result = service.handle(new SubscribeToPlanCommand(1L, "BASICO", "tok_approved"));

        assertThat(errorOf(result).code()).isEqualTo("SUBSCRIPTION_CONFLICT");
        verify(paymentGatewayService, never()).charge(any());
    }

    private static Plan plan(Long id, String code, String price) {
        var plan = new Plan(code, code, "desc", new BigDecimal(price), "PEN", 100, List.of("feature"));
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    private static ApplicationError errorOf(Result<?, ApplicationError> result) {
        if (result instanceof Result.Failure<?, ApplicationError> failure) {
            return failure.error();
        }
        throw new AssertionError("Se esperaba un fallo");
    }
}
