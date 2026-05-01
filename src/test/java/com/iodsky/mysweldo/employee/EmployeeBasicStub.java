package com.iodsky.mysweldo.employee;

import lombok.Builder;

@Builder
class EmployeeBasicStub implements EmployeeBasic {

    Long id;
    String firstName;
    String lastName;
    String departmentId;
    String departmentTitle;
    String positionId;
    String positionTitle;
    EmploymentStatus status;
    EmploymentType type;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getDepartment_Id() {
        return departmentId;
    }

    @Override
    public String getDepartment_Title() {
        return departmentTitle;
    }

    @Override
    public String getPosition_Id() {
        return positionId;
    }

    @Override
    public String getPosition_Title() {
        return positionTitle;
    }

    @Override
    public EmploymentStatus getStatus() {
        return status;
    }

    @Override
    public EmploymentType getType() {
        return type;
    }
}
