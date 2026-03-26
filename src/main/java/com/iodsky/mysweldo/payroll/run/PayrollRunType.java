package com.iodsky.mysweldo.payroll.run;

/**
 * Enumerates supported payroll types.
 *
 * Currently supported:
 * - REGULAR: Standard payroll processing
 * - OFF_CYCLE: Non-standard payroll (e.g., termination, bonus)
 * - ADJUSTMENT: Payroll adjustments
 *
 */
public enum PayrollRunType {
    REGULAR,
    OFF_CYCLE,
    ADJUSTMENT,
}
