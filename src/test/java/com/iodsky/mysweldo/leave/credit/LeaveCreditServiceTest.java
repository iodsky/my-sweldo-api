package com.iodsky.mysweldo.leave.credit;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.leave.LeaveType;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCreditServiceTest {

    @InjectMocks
    private LeaveCreditService service;

    @Mock
    private LeaveCreditRepository repository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private UserService userService;

    private Employee employee;
    private LocalDate effectiveDate;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(1L).build();
        effectiveDate = LocalDate.of(2025, 1, 1);
    }

    @Nested
    class CreateLeaveCreditsTests {

        @Test
        void shouldCreateThreeLeaveCreditsWhenNoneExistForEffectiveDate() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(false);
            when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<LeaveCredit> result = service.createLeaveCredits(dto);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(LeaveCredit::getType)
                    .containsExactlyInAnyOrder(LeaveType.VACATION, LeaveType.SICK, LeaveType.BEREAVEMENT);
        }

        @Test
        void shouldAssignDefaultCreditValuesForEachLeaveType() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(false);
            when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<LeaveCredit> result = service.createLeaveCredits(dto);

            LeaveCredit vacation = result.stream().filter(c -> c.getType() == LeaveType.VACATION).findFirst().orElseThrow();
            LeaveCredit sick = result.stream().filter(c -> c.getType() == LeaveType.SICK).findFirst().orElseThrow();
            LeaveCredit bereavement = result.stream().filter(c -> c.getType() == LeaveType.BEREAVEMENT).findFirst().orElseThrow();

            assertThat(vacation.getCredits()).isEqualTo(14.0);
            assertThat(sick.getCredits()).isEqualTo(7.0);
            assertThat(bereavement.getCredits()).isEqualTo(5.0);
        }

        @Test
        void shouldThrow409WhenLeaveCreditsAlreadyExistForEffectiveDate() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(true);

            assertThatThrownBy(() -> service.createLeaveCredits(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldPropagateExceptionWhenEmployeeDoesNotExist() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(99L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(99L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee 99 not found"));

            assertThatThrownBy(() -> service.createLeaveCredits(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetLeaveCreditByEmployeeIdAndTypeTests {

        @Test
        void shouldReturnLeaveCreditWhenItExistsForEmployeeAndType() {
            LeaveCredit credit = LeaveCredit.builder()
                    .employee(employee)
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .effectiveDate(effectiveDate)
                    .build();

            when(repository.findByEmployee_IdAndType(1L, LeaveType.VACATION)).thenReturn(Optional.of(credit));

            LeaveCredit result = service.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION);

            assertThat(result).isEqualTo(credit);
        }

        @Test
        void shouldThrow404WhenNoCreditExistsForEmployeeAndType() {
            when(repository.findByEmployee_IdAndType(1L, LeaveType.SICK)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.SICK))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetLeaveCreditsByEmployeeIdTests {

        @Test
        void shouldReturnAllLeaveCreditsForAuthenticatedEmployee() {
            User user = mock(User.class);
            when(user.getEmployee()).thenReturn(employee);
            when(userService.getAuthenticatedUser()).thenReturn(user);

            List<LeaveCredit> credits = List.of(
                    LeaveCredit.builder().type(LeaveType.VACATION).credits(14.0).build(),
                    LeaveCredit.builder().type(LeaveType.SICK).credits(7.0).build()
            );
            when(repository.findAllByEmployee_Id(1L)).thenReturn(credits);

            List<LeaveCredit> result = service.getLeaveCreditsByEmployeeId();

            assertThat(result).isEqualTo(credits);
        }
    }

    @Nested
    class UpdateLeaveCreditTests {

        @Test
        void shouldUpdateAndReturnCreditWithNewValueWhenCreditExists() {
            UUID id = UUID.randomUUID();
            LeaveCredit existing = LeaveCredit.builder()
                    .employee(employee)
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .effectiveDate(effectiveDate)
                    .build();
            LeaveCredit updated = LeaveCredit.builder().credits(10.0).build();

            when(repository.findById(id)).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            LeaveCredit result = service.updateLeaveCredit(id, updated);

            assertThat(result.getCredits()).isEqualTo(10.0);
        }

        @Test
        void shouldThrow404WhenCreditToUpdateDoesNotExist() {
            UUID id = UUID.randomUUID();
            LeaveCredit updated = LeaveCredit.builder().credits(10.0).build();

            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLeaveCredit(id, updated))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class DeleteLeaveCreditsByEmployeeIdTests {

        @Test
        void shouldSoftDeleteAllCreditsForEmployeeBySettingDeletedAt() {
            LeaveCredit credit1 = LeaveCredit.builder().type(LeaveType.VACATION).credits(14.0).build();
            LeaveCredit credit2 = LeaveCredit.builder().type(LeaveType.SICK).credits(7.0).build();

            when(repository.findAllByEmployee_Id(1L)).thenReturn(List.of(credit1, credit2));

            service.deleteLeaveCreditsByEmployeeId(1L);

            assertThat(credit1.getDeletedAt()).isNotNull();
            assertThat(credit2.getDeletedAt()).isNotNull();
            verify(repository).saveAll(List.of(credit1, credit2));
        }

        @Test
        void shouldSaveEmptyListWhenEmployeeHasNoCredits() {
            when(repository.findAllByEmployee_Id(1L)).thenReturn(List.of());

            service.deleteLeaveCreditsByEmployeeId(1L);

            verify(repository).saveAll(List.of());
        }
    }
}