package com.iodsky.mysweldo.payroll.run;

import lombok.Data;

import java.util.List;

@Data
public class GeneratePayrollRequest {

    List<Long> employeeIds;

}
