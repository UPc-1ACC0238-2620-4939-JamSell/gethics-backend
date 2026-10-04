package com.jamsell.gethics.finance.domain.model.entities;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "financial_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_management_id", nullable = false)
    private FinancialManagement financialManagement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialTransactionType type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(columnDefinition = "TEXT")
    private String description;

    public FinancialTransaction(FinancialManagement financialManagement, FinancialTransactionType type,
                                BigDecimal amount, String category, LocalDate occurredOn, String description) {
        this.financialManagement = financialManagement;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.occurredOn = occurredOn;
        this.description = description;
    }
}
