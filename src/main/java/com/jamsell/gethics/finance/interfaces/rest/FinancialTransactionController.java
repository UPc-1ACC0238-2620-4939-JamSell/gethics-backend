package com.jamsell.gethics.finance.interfaces.rest;

import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.services.FinancialManagementCommandService;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import com.jamsell.gethics.finance.interfaces.rest.resources.FinancialSummaryResource;
import com.jamsell.gethics.finance.interfaces.rest.resources.RegisterTransactionResource;
import com.jamsell.gethics.finance.interfaces.rest.resources.TransactionResource;
import com.jamsell.gethics.finance.interfaces.rest.transform.RegisterTransactionCommandFromResourceAssembler;
import com.jamsell.gethics.finance.interfaces.rest.transform.TransactionResourceFromEntityAssembler;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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

    private static final String NO_MOVEMENTS_MESSAGE = "No existen movimientos registrados para el periodo "
            + "seleccionado.";

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
    public ResponseEntity<FinancialSummaryResource> getSummary(
            @RequestParam UUID ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both 'from' and 'to' must be provided to filter by period");
        }

        var summary = queryService.handle(new GetFinancialSummaryByOwnerQuery(ownerId, from, to));
        var transactions = summary.transactions().stream()
                .map(TransactionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        var message = summary.hasMovements() ? null : NO_MOVEMENTS_MESSAGE;

        return ResponseEntity.ok(new FinancialSummaryResource(
                ownerId, summary.totalIncome(), summary.totalExpense(), summary.netBalance(), transactions,
                message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
