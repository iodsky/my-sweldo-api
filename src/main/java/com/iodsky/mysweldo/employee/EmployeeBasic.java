package com.iodsky.mysweldo.employee;

public interface EmployeeBasic {
    Long getId();
    String getFirstName();
    String getLastName();

    String getDepartment_Id();
    String getDepartment_Title();

    String getPosition_Id();
    String getPosition_Title();

    EmploymentStatus getStatus();
    EmploymentType getType();
}
