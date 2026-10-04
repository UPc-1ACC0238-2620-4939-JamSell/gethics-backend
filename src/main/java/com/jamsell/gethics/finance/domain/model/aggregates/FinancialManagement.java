package com.jamsell.gethics.finance.domain.model.aggregates;

import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "financial_managements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private Long version;

    @OneToMany(mappedBy = "financialManagement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialTransaction> transactions = new ArrayList<>();

    public FinancialManagement(UUID ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner id is required");
        }
        this.ownerId = ownerId;
    }

    public FinancialTransaction register(FinancialTransactionType type, BigDecimal amount,
                                         String category, LocalDate occurredOn, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        var transaction = new FinancialTransaction(this, type, amount, category, occurredOn, description);
        transactions.add(transaction);
        balance = type == FinancialTransactionType.INCOME
                ? balance.add(amount)
                : balance.subtract(amount);
        return transaction;
    }
}
