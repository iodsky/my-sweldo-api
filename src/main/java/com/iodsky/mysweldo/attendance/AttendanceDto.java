package com.iodsky.mysweldo.attendance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDto {
    private UUID id;
    private Long employeeId;
    private String employeeFirstName;
    private String employeeLastName;
    private LocalDate date;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private BigDecimal totalHours;
    private BigDecimal overtimeHours;
}
