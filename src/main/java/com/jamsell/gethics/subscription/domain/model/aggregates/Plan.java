package com.jamsell.gethics.subscription.domain.model.aggregates;

import com.jamsell.gethics.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "plans", uniqueConstraints = @UniqueConstraint(name = "uk_plans_code", columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends AuditableAbstractAggregateRoot<Plan> {

    public static final String FREE_CODE = "GRATUITO";

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int maxAnimals;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature", nullable = false, length = 120)
    private List<String> features = new ArrayList<>();

    public Plan(
            String code,
            String name,
            String description,
            BigDecimal price,
            String currency,
            int maxAnimals,
            List<String> features
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.maxAnimals = maxAnimals;
        this.features = new ArrayList<>(features);
    }

    public boolean isFree() {
        return price.signum() == 0;
    }
}
