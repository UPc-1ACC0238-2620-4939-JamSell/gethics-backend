package com.jamsell.gethics.subscription.application.internal.commandservices;

import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.application.result.Result;
import com.jamsell.gethics.subscription.application.internal.outboundservices.payment.PaymentGatewayService;
import com.jamsell.gethics.subscription.application.internal.outboundservices.payment.PaymentGatewayService.PaymentRequest;
import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import com.jamsell.gethics.subscription.domain.model.aggregates.Subscription;
import com.jamsell.gethics.subscription.domain.model.commands.SeedDefaultPlansCommand;
import com.jamsell.gethics.subscription.domain.model.commands.SubscribeToPlanCommand;
import com.jamsell.gethics.subscription.domain.model.valueobjects.CurrentSubscription;
import com.jamsell.gethics.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.jamsell.gethics.subscription.domain.services.SubscriptionCommandService;
import com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private static final Duration BILLING_PERIOD = Duration.ofDays(30);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGatewayService paymentGatewayService;

    public SubscriptionCommandServiceImpl(
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentGatewayService paymentGatewayService
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    @Override
    @Transactional
    public Result<CurrentSubscription, ApplicationError> handle(SubscribeToPlanCommand command) {
        var plan = planRepository.findByCode(command.planCode());
        if (plan.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plan", "El plan solicitado no existe"));
        }
        if (plan.get().isFree()) {
            return Result.failure(ApplicationError.businessRuleViolation(
                    "El plan gratuito está activo por defecto y no requiere suscripción"));
        }
        var now = Instant.now();
        var activeSubscriptions = subscriptionRepository.findByUserIdAndStatus(command.userId(), SubscriptionStatus.ACTIVE);
        var alreadySubscribed = activeSubscriptions.stream()
                .anyMatch(subscription -> subscription.isActiveAt(now) && subscription.getPlanId().equals(plan.get().getId()));
        if (alreadySubscribed) {
            return Result.failure(ApplicationError.conflict("Subscription", "Ya tienes una suscripción activa a este plan"));
        }
        var payment = paymentGatewayService.charge(new PaymentRequest(
                plan.get().getPrice(),
                plan.get().getCurrency(),
                command.paymentToken(),
                "Suscripción Gethics " + plan.get().getName()
        ));
        if (!payment.approved()) {
            return Result.failure(ApplicationError.paymentRejected(payment.failureReason()));
        }
        activeSubscriptions.forEach(Subscription::cancel);
        subscriptionRepository.saveAll(activeSubscriptions);
        var subscription = subscriptionRepository.save(new Subscription(
                command.userId(),
                plan.get().getId(),
                now,
                now.plus(BILLING_PERIOD),
                payment.reference()
        ));
        return Result.success(new CurrentSubscription(plan.get(), subscription));
    }

    @Override
    @Transactional
    public void handle(SeedDefaultPlansCommand command) {
        var free = List.of("Registro de animales", "Historial sanitario básico");
        var basic = List.of("Registro de animales", "Historial sanitario completo", "Calendario con alertas", "Control económico");
        var premium = List.of("Todo lo del plan Básico", "Reportes avanzados", "Módulo veterinario");
        seed(new Plan(Plan.FREE_CODE, "Gratuito", "Para ganaderos que recién comienzan", BigDecimal.ZERO, "PEN", 20, free));
        seed(new Plan("BASICO", "Básico", "Para pequeños ganaderos", new BigDecimal("19.90"), "PEN", 100, basic));
        seed(new Plan("PREMIUM", "Premium", "Para hatos grandes con funciones avanzadas", new BigDecimal("49.90"), "PEN", 500, premium));
    }

    private void seed(Plan plan) {
        if (!planRepository.existsByCode(plan.getCode())) {
            planRepository.save(plan);
        }
    }
}
