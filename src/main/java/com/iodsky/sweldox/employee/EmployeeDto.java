package com.iodsky.sweldox.employee;

import com.iodsky.sweldox.benefit.BenefitDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EmployeeDto {

    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate birthday;
    private String address;
    private String phoneNumber;
    private String sssNumber;
    private String tinNumber;
    private String philhealthNumber;
    private String pagIbigNumber;
    private String supervisor;
    private String position;
    private String department;
    private String status;
    private LocalTime startShift;
    private LocalTime endShift;
    private BigDecimal basicSalary;
    private BigDecimal hourlyRate;
    private BigDecimal semiMonthlyRate;
//    private Map<String, BigDecimal> benefits;
    private List<BenefitDto> benefits;

}
