package com.iodsky.mysweldo.payroll.run;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PayrollRunRequest {
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private PayrollRunType type;
    private String notes;
}
