package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for resolving the appropriate PayrollComputationStrategy based on payroll frequency.
 *
 * This component enables extensibility by allowing new payroll types to be added
 * by simply registering new strategy implementations.
 */
@Component
@RequiredArgsConstructor
public class PayrollStrategyFactory {

    private final SemiMonthlyPayrollStrategy semiMonthlyPayrollStrategy;
    // Future strategies can be injected here as needed
    // private final MonthlyPayrollStrategy monthlyPayrollStrategy;
    // private final WeeklyPayrollStrategy weeklyPayrollStrategy;
    // private final BiWeeklyPayrollStrategy biWeeklyPayrollStrategy;

    /**
     * Resolves the appropriate PayrollComputationStrategy for the given payroll frequency.
     *
     * @param frequency The payroll frequency
     * @return The corresponding PayrollComputationStrategy
     * @throws PayrollRunException if no strategy is found for the given frequency
     */
    public PayrollComputationStrategy getStrategy(PayrollFrequency frequency) {
        return switch (frequency) {
            case SEMI_MONTHLY -> semiMonthlyPayrollStrategy;
            case MONTHLY, WEEKLY, BI_WEEKLY -> throw new PayrollRunException(
                    "Payroll frequency " + frequency + " is not yet supported"
            );
        };
    }
}
