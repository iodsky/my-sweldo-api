package com.iodsky.mysweldo.employee;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "employee_benefit")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeBenefit extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "benefit_code")
    @JsonIgnore
    private Benefit benefit;

    private BigDecimal amount;

    public BigDecimal getTaxableAmount() {
        if (benefit.isTaxable()) {
            return amount;
        }
        if (benefit.getNonTaxableLimit() == null) {
            return amount;
        }

        return amount.subtract(benefit.getNonTaxableLimit()).max(BigDecimal.ZERO);
    }

    public BigDecimal getNonTaxableAmount() {
        if (!benefit.isTaxable() && benefit.getNonTaxableLimit() != null) {
            return amount.min(benefit.getNonTaxableLimit());
        }
        return BigDecimal.ZERO;
    }

}
