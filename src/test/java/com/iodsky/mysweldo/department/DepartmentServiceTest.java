package com.iodsky.mysweldo.department;

import com.iodsky.mysweldo.position.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @InjectMocks
    private DepartmentService service;

    @Mock
    private DepartmentRepository repository;

    @Mock
    private PositionRepository positionRepository;

    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id("ENGINEERING")
                .title("Engineering")
                .build();
    }

    @Nested
    class CreateDepartmentTests {

        @Test
        void shouldCreateDepartmentWhenIdDoesNotExist() {
            DepartmentRequest request = new DepartmentRequest();
            request.setId("ENGINEERING");
            request.setTitle("Engineering");

            when(repository.existsById("ENGINEERING")).thenReturn(false);
            when(repository.save(any(Department.class))).thenReturn(department);

            Department result = service.createDepartment(request);

            assertThat(result.getId()).isEqualTo("ENGINEERING");
            assertThat(result.getTitle()).isEqualTo("Engineering");
        }

        @Test
        void shouldThrowConflictWhenDepartmentIdAlreadyExists() {
            DepartmentRequest request = new DepartmentRequest();
            request.setId("ENGINEERING");
            request.setTitle("Engineering");

            when(repository.existsById("ENGINEERING")).thenReturn(true);

            assertThatThrownBy(() -> service.createDepartment(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("ENGINEERING");
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class GetAllDepartmentsTests {

        @Test
        void shouldReturnPaginatedDepartmentsWhenTheyExist() {
            Page<Department> page = new PageImpl<>(List.of(department));
            when(repository.findAll(any(Pageable.class))).thenReturn(page);

            Page<Department> result = service.getAllDepartments(0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo("ENGINEERING");
        }

        @Test
        void shouldReturnEmptyPageWhenNoDepartmentsExist() {
            Page<Department> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<Department> result = service.getAllDepartments(0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetDepartmentByIdTests {

        @Test
        void shouldReturnDepartmentWhenIdExists() {
            when(repository.findById("ENGINEERING")).thenReturn(Optional.of(department));

            Department result = service.getDepartmentById("ENGINEERING");

            assertThat(result.getId()).isEqualTo("ENGINEERING");
            assertThat(result.getTitle()).isEqualTo("Engineering");
        }

        @Test
        void shouldThrowNotFoundWhenDepartmentIdDoesNotExist() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDepartmentById("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("UNKNOWN");
                    });
        }
    }

    @Nested
    class UpdateDepartmentTests {

        @Test
        void shouldUpdateTitleWhenDepartmentExists() {
            DepartmentUpdateRequest request = new DepartmentUpdateRequest();
            request.setTitle("Engineering & R&D");

            Department updated = Department.builder()
                    .id("ENGINEERING")
                    .title("Engineering & R&D")
                    .build();

            when(repository.findById("ENGINEERING")).thenReturn(Optional.of(department));
            when(repository.save(any(Department.class))).thenReturn(updated);

            Department result = service.updateDepartment("ENGINEERING", request);

            assertThat(result.getTitle()).isEqualTo("Engineering & R&D");
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentDepartment() {
            DepartmentUpdateRequest request = new DepartmentUpdateRequest();
            request.setTitle("New Title");

            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDepartment("UNKNOWN", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class DeleteDepartmentTests {

        @Test
        void shouldSoftDeleteDepartmentWhenNoActiveEmployeesOrPositions() {
            when(repository.findById("ENGINEERING")).thenReturn(Optional.of(department));
            when(repository.countEmployeesByDepartmentId("ENGINEERING")).thenReturn(0L);
            when(positionRepository.existsByDepartmentId("ENGINEERING")).thenReturn(false);

            service.deleteDepartment("ENGINEERING");

            ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getDeletedAt()).isNotNull();
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentDepartment() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteDepartment("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowConflictWhenDepartmentHasActiveEmployees() {
            when(repository.findById("ENGINEERING")).thenReturn(Optional.of(department));
            when(repository.countEmployeesByDepartmentId("ENGINEERING")).thenReturn(3L);

            assertThatThrownBy(() -> service.deleteDepartment("ENGINEERING"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("3");
                        assertThat(rse.getReason()).contains("Engineering");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowConflictWhenDepartmentHasActivePositions() {
            when(repository.findById("ENGINEERING")).thenReturn(Optional.of(department));
            when(repository.countEmployeesByDepartmentId("ENGINEERING")).thenReturn(0L);
            when(positionRepository.existsByDepartmentId("ENGINEERING")).thenReturn(true);

            assertThatThrownBy(() -> service.deleteDepartment("ENGINEERING"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("Engineering");
                    });

            verify(repository, never()).save(any());
        }
    }
}
