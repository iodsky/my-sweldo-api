package com.iodsky.mysweldo.employee;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeBasicDto {
    private Long id;
    private String firstName;
    private String lastName;
    private DepartmentBasicDto department;
    private PositionBasicDto position;
    private EmploymentStatus status;
    private EmploymentType type;
}
