package com.iodsky.mysweldo.payroll.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneratePayrollResponse {

    private PayrollRunDto payrollRun;

    private List<Long> skippedEmployeeIds;

}
