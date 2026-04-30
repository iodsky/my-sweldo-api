package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalaryDto {
    private BigDecimal rate;
    private PayType payType;
    private PayrollFrequency payFrequency;
}
