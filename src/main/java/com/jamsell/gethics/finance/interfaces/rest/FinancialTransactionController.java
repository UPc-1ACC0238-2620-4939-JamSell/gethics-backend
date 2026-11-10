package com.jamsell.gethics.finance.interfaces.rest;

import com.jamsell.gethics.finance.domain.model.commands.VoidTransactionCommand;
import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.services.FinancialManagementCommandService;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import com.jamsell.gethics.finance.interfaces.rest.resources.FinancialSummaryResource;
import com.jamsell.gethics.finance.interfaces.rest.resources.RegisterTransactionResource;
import com.jamsell.gethics.finance.interfaces.rest.resources.TransactionResource;
import com.jamsell.gethics.finance.interfaces.rest.transform.RegisterTransactionCommandFromResourceAssembler;
import com.jamsell.gethics.finance.interfaces.rest.transform.TransactionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Finance")
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

    @Operation(summary = "Registrar un ingreso o egreso",
            description = "Crea un movimiento financiero del ganadero (INCOME o EXPENSE) con monto, fecha y categoria.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movimiento registrado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (monto <= 0 o campos obligatorios vacios)")
    })
    @PostMapping
    public ResponseEntity<TransactionResource> register(@Valid @RequestBody RegisterTransactionResource resource) {
        var command = RegisterTransactionCommandFromResourceAssembler.toCommandFromResource(resource);
        var transaction = commandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransactionResourceFromEntityAssembler.toResourceFromEntity(transaction));
    }

    @Operation(summary = "Balance economico del ganadero",
            description = "Devuelve ingresos, egresos, saldo y movimientos activos. Sin from/to usa todo el historial; "
                    + "con from y to filtra el periodo. Los movimientos anulados no cuentan. Si no hay movimientos "
                    + "devuelve 200 con un mensaje.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance calculado"),
        @ApiResponse(responseCode = "400", description = "Periodo invalido (falta from o to, o from es posterior a to)")
    })
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

    @Operation(summary = "Anular un movimiento financiero",
            description = "US-31: marca el movimiento como anulado sin borrarlo; deja de sumar en el balance.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Movimiento anulado"),
        @ApiResponse(responseCode = "400", description = "El movimiento no existe o ya estaba anulado")
    })
    @PostMapping("/{id}/void")
    public ResponseEntity<Void> voidTransaction(@PathVariable UUID id) {
        var command = new VoidTransactionCommand(id);
        commandService.handle(command);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
