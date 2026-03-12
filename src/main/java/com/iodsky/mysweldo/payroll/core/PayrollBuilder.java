package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.attendance.Attendance;
import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.contribution.ContributionService;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.overtime.OvertimeRequestService;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollBuilder {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final OvertimeRequestService overtimeRequestService;
    private final DeductionService deductionService;
    private final ContributionService contributionService;
    private final PayrollCalculator payrollCalculator;

    public PayrollItem buildPayroll(Long employeeId, PayrollRun run, PayrollConfiguration config) {
        PayrollContext context = buildContext(employeeId, run, config);
        return buildPayrollFromContext(context, run);
    }

    private PayrollContext buildContext(Long employeeId, PayrollRun run, PayrollConfiguration config) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        List<Attendance> attendances = attendanceService.getEmployeeAttendances(employeeId, run.getPeriodStartDate(), run.getPeriodEndDate());
        List<EmployeeBenefit> benefits = employee.getBenefits();

        BigDecimal basicSalary = employee.getBasicSalary();
        BigDecimal hourlyRate = employee.getHourlyRate();

        // Calculate hours
        BigDecimal totalHours = attendanceService.calculateTotalHoursByEmployeeId(employeeId, run.getPeriodStartDate(), run.getPeriodEndDate());
        BigDecimal approvedOvertimeHours = overtimeRequestService.calculateApprovedOvertimeHours(employeeId, run.getPeriodStartDate(), run.getPeriodEndDate());
        BigDecimal standardHours = BigDecimal.valueOf(attendances.size()).multiply(BigDecimal.valueOf(8));
        BigDecimal regularHours = totalHours.subtract(approvedOvertimeHours).min(standardHours);

        // Calculate pay
        BigDecimal regularPay = payrollCalculator.calculateRegularPay(hourlyRate, regularHours);
        BigDecimal overtimePay = payrollCalculator.calculateOvertimePay(hourlyRate, approvedOvertimeHours);
        BigDecimal grossPay = payrollCalculator.calculateGrossPay(regularPay, overtimePay);

        // Calculate benefits
        BigDecimal totalBenefits = payrollCalculator.calculateTotalBenefits(benefits);

        // Calculate statutory deductions using preloaded configuration
        BigDecimal sss = payrollCalculator.calculateSssDeduction(basicSalary, config.getSssRateTable());
        BigDecimal philhealth = payrollCalculator.calculatePhilhealthDeduction(basicSalary, config.getPhilhealthRateTable());
        BigDecimal pagibig = payrollCalculator.calculatePagibigDeduction(basicSalary, config.getPagibigRateTable());

        // Calculate employer contributions using preloaded configuration
        BigDecimal sssEr = payrollCalculator.calculateSssEmployerContribution(basicSalary, config.getSssRateTable());
        BigDecimal philhealthEr = payrollCalculator.calculatePhilhealthEmployerContribution(basicSalary, config.getPhilhealthRateTable());
        BigDecimal pagibigEr = payrollCalculator.calculatePagibigEmployerContribution(basicSalary, config.getPagibigRateTable());
        BigDecimal totalEmployerContributions = payrollCalculator.calculateTotalEmployerContributions(sssEr, philhealthEr, pagibigEr);

        // Calculate tax using preloaded configuration
        BigDecimal statutoryDeductions = payrollCalculator.calculateTotalStatutoryDeductions(sss, philhealth, pagibig);
        BigDecimal taxableIncome = payrollCalculator.calculateTaxableIncome(grossPay, statutoryDeductions);
        BigDecimal withholdingTax = payrollCalculator.calculateWithholdingTax(taxableIncome, config.getIncomeTaxBrackets());
        BigDecimal totalDeductions = withholdingTax.add(statutoryDeductions).setScale(2, RoundingMode.HALF_UP);

        // Calculate net pay
        BigDecimal netPay = payrollCalculator.calculateNetPay(grossPay, totalBenefits, statutoryDeductions, withholdingTax);

        return PayrollContext.builder()
                .employee(employee)
                .attendances(attendances)
                .employeeBenefits(benefits)
                .hourlyRate(hourlyRate)
                .basicSalary(basicSalary)
                .totalHours(totalHours)
                .overtimeHours(approvedOvertimeHours)
                .regularHours(regularHours)
                .regularPay(regularPay)
                .overtimePay(overtimePay)
                .grossPay(grossPay)
                .totalBenefits(totalBenefits)
                .sss(sss)
                .philhealth(philhealth)
                .pagibig(pagibig)
                .sssEr(sssEr)
                .philhealthEr(philhealthEr)
                .pagibigEr(pagibigEr)
                .totalEmployerContributions(totalEmployerContributions)
                .taxableIncome(taxableIncome)
                .withholdingTax(withholdingTax)
                .totalDeductions(totalDeductions)
                .netPay(netPay)
                .build();
    }

    private PayrollItem buildPayrollFromContext(PayrollContext context, PayrollRun payrollRun) {
        BigDecimal dailyRate = payrollCalculator.calculateDailyRate(context.getHourlyRate());

        List<PayrollDeduction> deductions = buildDeductions(context);
        List<PayrollBenefit> payrollBenefits = buildPayrollBenefits(context.getEmployeeBenefits());
        List<EmployerContribution> employerContributions = buildEmployerContributions(context);

        PayrollItem payroll = PayrollItem.builder()
                .payrollRun(payrollRun)
                .employee(context.getEmployee())
                .monthlyRate(context.getBasicSalary())
                .dailyRate(dailyRate)
                .daysWorked(context.getAttendances().size())
                .overtime(context.getOvertimeHours())
                .grossPay(context.getGrossPay())
                .benefits(payrollBenefits)
                .totalBenefits(context.getTotalBenefits())
                .deductions(deductions)
                .totalDeductions(context.getTotalDeductions())
                .employerContributions(employerContributions)
                .totalEmployerContributions(context.getTotalEmployerContributions())
                .netPay(context.getNetPay())
                .build();

        deductions.forEach(d -> d.setPayroll(payroll));
        payrollBenefits.forEach(b -> b.setPayroll(payroll));
        employerContributions.forEach(c -> c.setPayrollItem(payroll));

        return payroll;
    }

    private List<PayrollDeduction> buildDeductions(PayrollContext context) {
        List<PayrollDeduction> deductions = new ArrayList<>();

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("SSS"))
                .amount(context.getSss())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("PHIC"))
                .amount(context.getPhilhealth())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("HDMF"))
                .amount(context.getPagibig())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("TAX"))
                .amount(context.getWithholdingTax())
                .build());

        return deductions;
    }

    private List<PayrollBenefit> buildPayrollBenefits(List<EmployeeBenefit> employeeBenefits) {
        return employeeBenefits.stream()
                .map(employeeBenefit -> PayrollBenefit.builder()
                        .benefit(employeeBenefit.getBenefit())
                        .amount(employeeBenefit.getAmount())
                        .build())
                .toList();
    }

    private List<EmployerContribution> buildEmployerContributions(PayrollContext context) {
        List<EmployerContribution> contributions = new ArrayList<>();

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("SSS_ER"))
                .amount(context.getSssEr())
                .build());

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("PHIC_ER"))
                .amount(context.getPhilhealthEr())
                .build());

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("HDMF_ER"))
                .amount(context.getPagibigEr())
                .build());

        return contributions;
    }

}
