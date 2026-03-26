package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.payroll.core.PayrollConfiguration;
import com.iodsky.mysweldo.payroll.core.PayrollContext;
import com.iodsky.mysweldo.payroll.run.PayrollRun;

public interface PayrollComputationStrategy {
    /**
     * Computes a PayrollContext for an employee within a PayrollRun.
     * @param employee Employee to compute payroll for
     * @param payrollRun The current payroll run
     * @param config Payroll configuration (statutory rates, tax brackets)
     * @return PayrollContext containing all calculated fields
     */
    PayrollContext compute(Employee employee, PayrollRun payrollRun, PayrollConfiguration config);
}
