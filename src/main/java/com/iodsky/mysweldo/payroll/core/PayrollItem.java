package com.iodsky.mysweldo.payroll.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "payroll_item",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"payroll_run_id", "employee_id"}
        )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollItem extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PayrollDeduction> deductions;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PayrollBenefit> benefits;

    @OneToMany(mappedBy = "payrollItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EmployerContribution> employerContributions;

    @Column(name = "days_worked")
    private int daysWorked;

    private BigDecimal overtime;

    @Column(name = "monthly_rate")
    private BigDecimal monthlyRate;

    @Column(name = "daily_rate")
    private BigDecimal dailyRate;

    @Column(name = "gross_pay")
    private BigDecimal grossPay;

    @Column(name = "total_benefits")
    private BigDecimal totalBenefits;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "total_employer_contributions")
    private BigDecimal totalEmployerContributions;

    @Column(name = "net_pay")
    private BigDecimal netPay;

}
