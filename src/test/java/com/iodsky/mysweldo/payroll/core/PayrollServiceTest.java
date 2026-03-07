package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.employee.Employee;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @InjectMocks
    private PayrollService service;

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private PayrollBuilder payrollBuilder;

    @Mock
    private UserService userService;

    private Employee employee;
    private User payrollUser;
    private User nonPayrollUser;
    private User differentEmployeePayrollUser;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(1L).build();

        payrollUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(new Role("PAYROLL"))
                .build();

        nonPayrollUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(new Role("EMPLOYEE"))
                .build();

        Employee otherEmployee = Employee.builder().id(2L).build();
        differentEmployeePayrollUser = User.builder()
                .id(UUID.randomUUID())
                .employee(otherEmployee)
                .role(new Role("PAYROLL"))
                .build();
    }

    @Nested
    class CreatePayrollTests {

        private final LocalDate periodStart = LocalDate.of(2025, 3, 1);
        private final LocalDate periodEnd = LocalDate.of(2025, 3, 15);
        private final LocalDate payDate = LocalDate.of(2025, 3, 20);

        @Test
        void shouldCreateAndReturnPayrollWhenNoDuplicateExists() {
            Payroll builtPayroll = Payroll.builder().employee(employee).build();
            Payroll savedPayroll = Payroll.builder().employee(employee).build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(1L, periodStart, periodEnd))
                    .thenReturn(false);
            when(payrollBuilder.buildPayroll(1L, periodStart, periodEnd, payDate)).thenReturn(builtPayroll);
            when(payrollRepository.save(builtPayroll)).thenReturn(savedPayroll);

            Payroll result = service.createPayroll(1L, periodStart, periodEnd, payDate);

            assertThat(result).isEqualTo(savedPayroll);
        }

        @Test
        void shouldThrowConflictWhenPayrollAlreadyExistsForEmployeeAndPeriod() {
            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(1L, periodStart, periodEnd))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createPayroll(1L, periodStart, periodEnd, payDate))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONFLICT));

            verify(payrollRepository, never()).save(any());
        }
    }

    @Nested
    class GetPayrollByIdTests {

        private UUID payrollId;
        private Payroll payroll;

        @BeforeEach
        void setUp() {
            payrollId = UUID.randomUUID();
            payroll = Payroll.builder().employee(employee).build();
        }

        @Test
        void shouldReturnPayrollWhenUserHasPayrollRoleAndOwnsThePayroll() {
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

            Payroll result = service.getPayrollById(payrollId);

            assertThat(result).isEqualTo(payroll);
        }

        @Test
        void shouldThrowNotFoundWhenPayrollDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findById(payrollId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPayrollById(payrollId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowForbiddenWhenUserDoesNotHavePayrollRole() {
            when(userService.getAuthenticatedUser()).thenReturn(nonPayrollUser);
            when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

            assertThatThrownBy(() -> service.getPayrollById(payrollId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        void shouldThrowForbiddenWhenPayrollUserDoesNotOwnThePayroll() {
            when(userService.getAuthenticatedUser()).thenReturn(differentEmployeePayrollUser);
            when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

            assertThatThrownBy(() -> service.getPayrollById(payrollId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        void shouldThrowForbiddenWhenUserHasNeitherPayrollRoleNorOwnsThePayroll() {
            Employee otherEmployee = Employee.builder().id(99L).build();
            User unrelatedUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(otherEmployee)
                    .role(new Role("EMPLOYEE"))
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(unrelatedUser);
            when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

            assertThatThrownBy(() -> service.getPayrollById(payrollId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Nested
    class GetAllPayrollTests {

        @Test
        void shouldReturnPaginatedPayrollsWithDescendingCreatedAtSortWhenNoPeriodProvided() {
            Page<Payroll> expectedPage = new PageImpl<>(List.of(Payroll.builder().build()));
            when(payrollRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

            Page<Payroll> result = service.getAllPayroll(0, 10, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldReturnPayrollsFilteredByPeriodWhenPeriodIsProvided() {
            YearMonth period = YearMonth.of(2025, 3);
            LocalDate expectedStart = LocalDate.of(2025, 3, 1);
            LocalDate expectedEnd = LocalDate.of(2025, 3, 31);

            Page<Payroll> expectedPage = new PageImpl<>(List.of(Payroll.builder().build()));
            when(payrollRepository.findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(expectedEnd), eq(expectedStart), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Payroll> result = service.getAllPayroll(0, 10, period);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldUseFirstAndLastDayOfMonthAsDateBoundsWhenFilteringByPeriod() {
            YearMonth february = YearMonth.of(2025, 2);
            LocalDate expectedStart = LocalDate.of(2025, 2, 1);
            LocalDate expectedEnd = LocalDate.of(2025, 2, 28);

            Page<Payroll> expectedPage = new PageImpl<>(List.of());
            when(payrollRepository.findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(expectedEnd), eq(expectedStart), any(Pageable.class)))
                    .thenReturn(expectedPage);

            service.getAllPayroll(0, 10, february);

            verify(payrollRepository).findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(expectedEnd), eq(expectedStart), any(Pageable.class));
        }
    }

    @Nested
    class GetAllEmployeePayrollTests {

        @Test
        void shouldReturnPaginatedPayrollsForAuthenticatedEmployeeWhenNoPeriodProvided() {
            Page<Payroll> expectedPage = new PageImpl<>(List.of(Payroll.builder().build()));
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findAllByEmployee_Id(eq(1L), any(Pageable.class))).thenReturn(expectedPage);

            Page<Payroll> result = service.getAllEmployeePayroll(0, 10, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldReturnPayrollsForAuthenticatedEmployeeFilteredByPeriodWhenPeriodIsProvided() {
            YearMonth period = YearMonth.of(2025, 3);
            LocalDate expectedStart = LocalDate.of(2025, 3, 1);
            LocalDate expectedEnd = LocalDate.of(2025, 3, 31);

            Page<Payroll> expectedPage = new PageImpl<>(List.of(Payroll.builder().build()));
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(1L), eq(expectedEnd), eq(expectedStart), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Payroll> result = service.getAllEmployeePayroll(0, 10, period);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldUseFirstAndLastDayOfMonthAsDateBoundsWhenFilteringEmployeePayrollByPeriod() {
            YearMonth february = YearMonth.of(2025, 2);
            LocalDate expectedStart = LocalDate.of(2025, 2, 1);
            LocalDate expectedEnd = LocalDate.of(2025, 2, 28);

            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(1L), eq(expectedEnd), eq(expectedStart), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.getAllEmployeePayroll(0, 10, february);

            verify(payrollRepository).findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(1L), eq(expectedEnd), eq(expectedStart), any(Pageable.class));
        }
    }
}

