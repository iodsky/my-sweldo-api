package com.iodsky.mysweldo.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface AttendanceView {
    UUID getId();
    String getEmployee_FirstName();
    String getEmployee_LastName();
    LocalDate getDate();
    LocalTime getTimeIn();
    LocalTime getTimeOut();
    BigDecimal getTotalHours();
    BigDecimal getOvertime();
}
