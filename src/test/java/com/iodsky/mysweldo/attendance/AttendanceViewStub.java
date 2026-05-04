package com.iodsky.mysweldo.attendance;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public class AttendanceViewStub implements AttendanceView {
    private UUID id;
    private String Employee_FirstName;
    private String Employee_LastName;
    private LocalDate date;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private BigDecimal totalHours;
    private BigDecimal overtime;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getEmployee_FirstName() {
        return Employee_FirstName;
    }

    @Override
    public String getEmployee_LastName() {
        return Employee_LastName;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public LocalTime getTimeIn() {
        return timeIn;
    }

    @Override
    public LocalTime getTimeOut() {
        return timeOut;
    }

    @Override
    public BigDecimal getTotalHours() {
        return totalHours;
    }

    @Override
    public BigDecimal getOvertime() {
        return overtime;
    }
}
