package com.jamsell.gethics.finance.interfaces.rest;

import com.jamsell.gethics.finance.domain.model.queries.GetBalanceByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.queries.GetTransactionsByOwnerQuery;
import com.jamsell.gethics.finance.domain.services.FinancialManagementCommandService;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import com.jamsell.gethics.finance.interfaces.rest.resources.FinancialSummaryResource;
import com.jamsell.gethics.finance.interfaces.rest.resources.RegisterTransactionResource;
import com.jamsell.gethics.finance.interfaces.rest.resources.TransactionResource;
import com.jamsell.gethics.finance.interfaces.rest.transform.RegisterTransactionCommandFromResourceAssembler;
import com.jamsell.gethics.finance.interfaces.rest.transform.TransactionResourceFromEntityAssembler;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finances")
public class FinancialTransactionController {

    private final FinancialManagementCommandService commandService;
    private final FinancialManagementQueryService queryService;

    public FinancialTransactionController(FinancialManagementCommandService commandService,
                                          FinancialManagementQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<TransactionResource> register(@Valid @RequestBody RegisterTransactionResource resource) {
        var command = RegisterTransactionCommandFromResourceAssembler.toCommandFromResource(resource);
        var transaction = commandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransactionResourceFromEntityAssembler.toResourceFromEntity(transaction));
    }

    @GetMapping
    public ResponseEntity<FinancialSummaryResource> getSummary(@RequestParam UUID ownerId) {
        var transactions = queryService.handle(new GetTransactionsByOwnerQuery(ownerId)).stream()
                .map(TransactionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        var balance = queryService.handle(new GetBalanceByOwnerQuery(ownerId));
        return ResponseEntity.ok(new FinancialSummaryResource(ownerId, balance, transactions));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
