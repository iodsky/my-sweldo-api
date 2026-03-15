package com.iodsky.mysweldo.batch.employee;


import com.iodsky.mysweldo.batch.DateTimeUtil;
import com.iodsky.mysweldo.employee.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeImportRecord {

    public static final String[] CSV_COLUMN_NAMES = {
            "lastName",
            "firstName",
            "birthday",
            "address",
            "phoneNumber",
            "sssNumber",
            "philhealthNumber",
            "tinNumber",
            "pagIbigNumber",
            "status",
            "type",
            "position",
            "supervisorId",
            "startShift",
            "endShift",
            "basicSalary",
            "mealAllowance",
            "phoneAllowance",
            "clothingAllowance"
    };

    private String lastName;
    private String firstName;
    private String birthday;
    private String address;
    private String phoneNumber;
    private String sssNumber;
    private String philhealthNumber;
    private String tinNumber;
    private String pagIbigNumber;
    private String status;
    private String type;
    private String position;
    private String supervisorId;
    private String startShift;
    private String endShift;
    private String basicSalary;
    private String mealAllowance;
    private String phoneAllowance;
    private String clothingAllowance;

    public static Employee toEntity(EmployeeImportRecord record) {

        GovernmentId governmentId = GovernmentId.builder()
                .sssNumber(record.getSssNumber())
                .philhealthNumber(record.getPhilhealthNumber())
                .tinNumber(record.getTinNumber())
                .pagIbigNumber(record.getPagIbigNumber())
                .build();

        Salary salary = Salary.builder()
                .type(SalaryType.MONTHLY)
                .amount(new BigDecimal(record.getBasicSalary()))
                .build();

        Employee employee = Employee.builder()
                .lastName(record.getLastName())
                .firstName(record.getFirstName())
                .birthday(DateTimeUtil.parseDate(record.getBirthday()))
                .address(record.getAddress())
                .phoneNumber(record.getPhoneNumber())
                .governmentId(governmentId)
                .salary(salary)
                .status(EmploymentStatus.valueOf(record.getStatus().toUpperCase()))
                .type(EmploymentType.valueOf(record.getType().toUpperCase()))
                .startShift(DateTimeUtil.parseTime(record.getStartShift()))
                .endShift(DateTimeUtil.parseTime(record.getEndShift()))
                .build();

        governmentId.setEmployee(employee);
        salary.setEmployee(employee);

        return employee;
    }

}
