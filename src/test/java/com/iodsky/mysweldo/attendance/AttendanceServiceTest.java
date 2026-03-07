package com.iodsky.mysweldo.attendance;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks
    private AttendanceService service;

    @Mock
    private AttendanceRepository repository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private UserService userService;

    private User hrUser;
    private User regularUser;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Role hrRole = new Role("HR");
        Role employeeRole = new Role("EMPLOYEE");

        employee = Employee.builder()
                .id(1L)
                .startShift(LocalTime.of(9, 0))
                .endShift(LocalTime.of(18, 0))
                .build();

        hrUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(hrRole)
                .build();

        Employee otherEmployee = Employee.builder().id(2L).build();
        regularUser = User.builder()
                .id(UUID.randomUUID())
                .employee(otherEmployee)
                .role(employeeRole)
                .build();
    }

    @Nested
    class CreateAttendanceTests {

        @Test
        void shouldClockInAuthenticatedUserWhenDtoIsNull() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDate(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.createAttendance(null);

            assertThat(result.getEmployee()).isEqualTo(employee);
            assertThat(result.getTimeOut()).isEqualTo(LocalTime.MIN);
            assertThat(result.getTotalHours()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getOvertime()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldClockInAuthenticatedUserWhenDtoEmployeeIdIsNull() {
            AttendanceDto dto = AttendanceDto.builder().build();
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDate(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.createAttendance(dto);

            assertThat(result.getEmployee()).isEqualTo(employee);
        }

        @Test
        void shouldUseProvidedDateAndTimeInWhenSuppliedInDto() {
            LocalDate targetDate = LocalDate.of(2025, 6, 10);
            LocalTime targetTime = LocalTime.of(8, 30);
            AttendanceDto dto = AttendanceDto.builder()
                    .date(targetDate)
                    .timeIn(targetTime)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDate(1L, targetDate)).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.createAttendance(dto);

            assertThat(result.getDate()).isEqualTo(targetDate);
            assertThat(result.getTimeIn()).isEqualTo(targetTime);
        }

        @Test
        void shouldAllowHrToCreateAttendanceForAnotherEmployee() {
            Employee target = Employee.builder().id(5L).build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(5L).build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDate(eq(5L), any(LocalDate.class))).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(5L)).thenReturn(target);
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.createAttendance(dto);

            assertThat(result.getEmployee()).isEqualTo(target);
        }

        @Test
        void shouldThrow403WhenNonHrUserTriesToCreateAttendanceForAnotherEmployee() {
            AttendanceDto dto = AttendanceDto.builder().employeeId(99L).build();
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createAttendance(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow409WhenAttendanceRecordAlreadyExistsForToday() {
            Attendance existing = Attendance.builder().build();
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDate(eq(1L), any(LocalDate.class))).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.createAttendance(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class GetEmployeeAttendanceByDateTests {

        @Test
        void shouldReturnAttendanceWhenRecordExists() {
            LocalDate date = LocalDate.of(2025, 6, 10);
            Attendance attendance = Attendance.builder().date(date).build();
            when(repository.findByEmployee_IdAndDate(1L, date)).thenReturn(Optional.of(attendance));

            Attendance result = service.getEmployeeAttendanceByDate(1L, date);

            assertThat(result).isEqualTo(attendance);
        }

        @Test
        void shouldReturnNullWhenNoRecordExists() {
            LocalDate date = LocalDate.of(2025, 6, 10);
            when(repository.findByEmployee_IdAndDate(1L, date)).thenReturn(Optional.empty());

            Attendance result = service.getEmployeeAttendanceByDate(1L, date);

            assertThat(result).isNull();
        }
    }

    @Nested
    class UpdateAttendanceTests {

        private UUID attendanceId;
        private Attendance attendance;

        @BeforeEach
        void setUp() {
            attendanceId = UUID.randomUUID();
            attendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .date(LocalDate.now())
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.MIN)
                    .build();
        }

        @Test
        void shouldClockOutAuthenticatedEmployeeWhenDtoIsNull() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.updateAttendance(attendanceId, null);

            assertThat(result.getTimeOut()).isNotNull();
            assertThat(result.getTimeOut()).isNotEqualTo(LocalTime.MIN);
        }

        @Test
        void shouldThrow404WhenAttendanceNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrow403WhenNonHrUserTriesToUpdateAnotherEmployeesAttendance() {
            Employee anotherEmployee = Employee.builder().id(99L).build();
            Attendance otherAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(anotherEmployee)
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.MIN)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(otherAttendance));

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow409WhenEmployeeAlreadyClockedOut() {
            attendance.setTimeOut(LocalTime.of(18, 0));
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldThrow403WhenNonHrUserProvidesManualDto() {
            Employee ownEmployee = Employee.builder().id(2L).build();
            Attendance ownAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(ownEmployee)
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.MIN)
                    .build();

            AttendanceDto dto = AttendanceDto.builder().timeOut(LocalTime.of(18, 0)).build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(ownAttendance));

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldAllowHrToUpdateTimeInTimeOutAndDate() {
            LocalTime newTimeIn = LocalTime.of(8, 0);
            LocalTime newTimeOut = LocalTime.of(17, 0);
            LocalDate newDate = LocalDate.of(2025, 6, 5);
            AttendanceDto dto = AttendanceDto.builder()
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .date(newDate)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.updateAttendance(attendanceId, dto);

            assertThat(result.getTimeIn()).isEqualTo(newTimeIn);
            assertThat(result.getTimeOut()).isEqualTo(newTimeOut);
            assertThat(result.getDate()).isEqualTo(newDate);
        }

        @Test
        void shouldCalculateTotalHoursAndOvertimeAfterClockOut() {
            attendance.getEmployee().setStartShift(LocalTime.of(9, 0));
            attendance.getEmployee().setEndShift(LocalTime.of(18, 0));

            AttendanceDto dto = AttendanceDto.builder().timeOut(LocalTime.of(20, 0)).build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.updateAttendance(attendanceId, dto);

            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("11.00"));
            assertThat(result.getOvertime()).isEqualByComparingTo(new BigDecimal("2.00"));
        }

        @Test
        void shouldSetOvertimeToZeroWhenWorkedHoursDoNotExceedRegularShift() {
            attendance.getEmployee().setStartShift(LocalTime.of(9, 0));
            attendance.getEmployee().setEndShift(LocalTime.of(18, 0));

            AttendanceDto dto = AttendanceDto.builder().timeOut(LocalTime.of(16, 0)).build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            Attendance result = service.updateAttendance(attendanceId, dto);

            assertThat(result.getOvertime()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldThrow400WhenEmployeeShiftIsNotConfigured() {
            attendance.getEmployee().setStartShift(null);
            attendance.getEmployee().setEndShift(null);

            AttendanceDto dto = AttendanceDto.builder().timeOut(LocalTime.of(18, 0)).build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class GetAllAttendancesTests {

        @Test
        void shouldReturnPagedAttendancesWithDescendingCreatedAtSort() {
            Page<Attendance> expectedPage = new PageImpl<>(List.of(Attendance.builder().build()));
            when(repository.findAllByDateBetween(any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Attendance> result = service.getAllAttendances(0, 10, LocalDate.now().withDayOfMonth(1), LocalDate.now());

            assertThat(result).isEqualTo(expectedPage);
            verify(repository).findAllByDateBetween(any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
        }

        @Test
        void shouldApplyDefaultDateRangeWhenNullDatesAreProvided() {
            Page<Attendance> expectedPage = new PageImpl<>(List.of());
            when(repository.findAllByDateBetween(any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Attendance> result = service.getAllAttendances(0, 10, null, null);

            assertThat(result).isEqualTo(expectedPage);
        }
    }

    @Nested
    class GetEmployeeAttendancesPaginatedTests {

        @Test
        void shouldReturnOwnAttendancesWhenEmployeeIdIsNull() {
            Page<Attendance> expectedPage = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Attendance> result = service.getEmployeeAttendances(0, 10, null, null, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldAllowHrToReadAnotherEmployeesAttendances() {
            Page<Attendance> expectedPage = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDateBetween(eq(5L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Attendance> result = service.getEmployeeAttendances(0, 10, 5L, null, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldAllowPayrollRoleToReadAnotherEmployeesAttendances() {
            Role payrollRole = new Role("PAYROLL");
            User payrollUser = User.builder()
                    .employee(employee)
                    .role(payrollRole)
                    .build();

            Page<Attendance> expectedPage = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(repository.findByEmployee_IdAndDateBetween(eq(5L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Attendance> result = service.getEmployeeAttendances(0, 10, 5L, null, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldAllowEmployeeToReadTheirOwnAttendances() {
            Page<Attendance> expectedPage = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findByEmployee_IdAndDateBetween(eq(2L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Attendance> result = service.getEmployeeAttendances(0, 10, 2L, null, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldThrow403WhenNonAdminUserAccessesAnotherEmployeesAttendances() {
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.getEmployeeAttendances(0, 10, 99L, null, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class GetEmployeeAttendancesListTests {

        @Test
        void shouldReturnListOfAttendancesForGivenEmployeeAndDateRange() {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);
            List<Attendance> expected = List.of(Attendance.builder().build());
            when(repository.findByEmployee_IdAndDateBetween(1L, start, end)).thenReturn(expected);

            List<Attendance> result = service.getEmployeeAttendances(1L, start, end);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class CalculateTotalHoursByEmployeeIdTests {

        @Test
        void shouldReturnSummedTotalHoursFromRepository() {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);
            BigDecimal expected = new BigDecimal("80.00");
            when(repository.sumTotalHoursByEmployee_IdAndDateBetween(1L, start, end)).thenReturn(expected);

            BigDecimal result = service.calculateTotalHoursByEmployeeId(1L, start, end);

            assertThat(result).isEqualByComparingTo(expected);
        }
    }
}

