package com.jamsell.gethics.subscription.interfaces.rest;

import com.jamsell.gethics.iam.infrastructure.security.OnlyGanadero;
import com.jamsell.gethics.iam.infrastructure.security.UserDetailsImpl;
import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.jamsell.gethics.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.jamsell.gethics.subscription.domain.model.commands.SubscribeToPlanCommand;
import com.jamsell.gethics.subscription.domain.model.queries.GetCurrentSubscriptionByUserIdQuery;
import com.jamsell.gethics.subscription.domain.services.SubscriptionCommandService;
import com.jamsell.gethics.subscription.domain.services.SubscriptionQueryService;
import com.jamsell.gethics.subscription.interfaces.rest.resources.SubscribeResource;
import com.jamsell.gethics.subscription.interfaces.rest.transform.CurrentSubscriptionResourceFromEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionController(
            SubscriptionCommandService subscriptionCommandService,
            SubscriptionQueryService subscriptionQueryService
    ) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @OnlyGanadero
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody SubscribeResource resource
    ) {
        var command = new SubscribeToPlanCommand(principal.getId(), resource.planCode(), resource.paymentToken());
        var result = subscriptionCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                CurrentSubscriptionResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentSubscription(@AuthenticationPrincipal UserDetailsImpl principal) {
        return subscriptionQueryService.handle(new GetCurrentSubscriptionByUserIdQuery(principal.getId()))
                .<ResponseEntity<?>>map(current -> ResponseEntity.ok(
                        CurrentSubscriptionResourceFromEntityAssembler.toResourceFromEntity(current)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Plan", "No se encontró un plan para el usuario")));
    }
}
