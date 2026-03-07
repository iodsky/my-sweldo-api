package com.iodsky.mysweldo.position;

import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @InjectMocks
    private PositionService service;

    @Mock
    private PositionRepository repository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private PositionMapper mapper;

    private Department department;
    private Position position;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id("ENG")
                .title("Engineering")
                .build();

        position = Position.builder()
                .id("SWE")
                .title("Software Engineer")
                .department(department)
                .build();
    }

    @Nested
    class CreatePositionTests {

        @Test
        void shouldCreatePositionWhenIdDoesNotExistAndDepartmentIsFound() {
            PositionRequest request = new PositionRequest();
            request.setId("SWE");
            request.setDepartmentId("ENG");
            request.setTitle("Software Engineer");

            when(repository.existsById("SWE")).thenReturn(false);
            when(departmentService.getDepartmentById("ENG")).thenReturn(department);
            when(mapper.toEntity(request)).thenReturn(Position.builder().id("SWE").title("Software Engineer").build());
            when(repository.save(any(Position.class))).thenReturn(position);

            Position result = service.createPosition(request);

            assertThat(result.getId()).isEqualTo("SWE");
            assertThat(result.getTitle()).isEqualTo("Software Engineer");
            assertThat(result.getDepartment().getId()).isEqualTo("ENG");
        }

        @Test
        void shouldThrowConflictWhenPositionIdAlreadyExists() {
            PositionRequest request = new PositionRequest();
            request.setId("SWE");
            request.setDepartmentId("ENG");
            request.setTitle("Software Engineer");

            when(repository.existsById("SWE")).thenReturn(true);

            assertThatThrownBy(() -> service.createPosition(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("SWE");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenDepartmentIsNotFound() {
            PositionRequest request = new PositionRequest();
            request.setId("SWE");
            request.setDepartmentId("UNKNOWN");
            request.setTitle("Software Engineer");

            when(repository.existsById("SWE")).thenReturn(false);
            when(departmentService.getDepartmentById("UNKNOWN"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Department UNKNOWN not found"));

            assertThatThrownBy(() -> service.createPosition(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class GetAllPositionsTests {

        @Test
        void shouldReturnPaginatedPositionsWhenTheyExist() {
            Page<Position> page = new PageImpl<>(List.of(position));
            when(repository.findAll(any(Pageable.class))).thenReturn(page);

            Page<Position> result = service.getAllPositions(0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo("SWE");
        }

        @Test
        void shouldReturnEmptyPageWhenNoPositionsExist() {
            Page<Position> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<Position> result = service.getAllPositions(0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetPositionByIdTests {

        @Test
        void shouldReturnPositionWhenIdExists() {
            when(repository.findById("SWE")).thenReturn(Optional.of(position));

            Position result = service.getPositionById("SWE");

            assertThat(result.getId()).isEqualTo("SWE");
            assertThat(result.getTitle()).isEqualTo("Software Engineer");
        }

        @Test
        void shouldThrowNotFoundWhenPositionDoesNotExist() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPositionById("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("UNKNOWN");
                    });
        }
    }

    @Nested
    class UpdatePositionTests {

        @Test
        void shouldUpdatePositionWhenDepartmentIsUnchangedAndHasNoEmployees() {
            PositionUpdateRequest request = new PositionUpdateRequest();
            request.setDepartmentId("ENG");
            request.setTitle("Senior Software Engineer");

            when(repository.findById("SWE")).thenReturn(Optional.of(position));
            when(departmentService.getDepartmentById("ENG")).thenReturn(department);
            when(repository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

            Position result = service.updatePosition("SWE", request);

            assertThat(result.getTitle()).isEqualTo("Senior Software Engineer");
            assertThat(result.getDepartment().getId()).isEqualTo("ENG");
        }

        @Test
        void shouldAllowDepartmentChangeWhenPositionHasNoActiveEmployees() {
            Department newDepartment = Department.builder().id("OPS").title("Operations").build();

            PositionUpdateRequest request = new PositionUpdateRequest();
            request.setDepartmentId("OPS");
            request.setTitle("Software Engineer");

            when(repository.findById("SWE")).thenReturn(Optional.of(position));
            when(departmentService.getDepartmentById("OPS")).thenReturn(newDepartment);
            when(repository.countEmployeesByPositionId("SWE")).thenReturn(0L);
            when(repository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

            Position result = service.updatePosition("SWE", request);

            assertThat(result.getDepartment().getId()).isEqualTo("OPS");
        }

        @Test
        void shouldThrowConflictWhenChangingDepartmentOfPositionWithActiveEmployees() {
            PositionUpdateRequest request = new PositionUpdateRequest();
            request.setDepartmentId("OPS");
            request.setTitle("Software Engineer");

            when(repository.findById("SWE")).thenReturn(Optional.of(position));
            when(departmentService.getDepartmentById("OPS"))
                    .thenReturn(Department.builder().id("OPS").title("Operations").build());
            when(repository.countEmployeesByPositionId("SWE")).thenReturn(3L);

            assertThatThrownBy(() -> service.updatePosition("SWE", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("Software Engineer");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenPositionToUpdateIsNotFound() {
            PositionUpdateRequest request = new PositionUpdateRequest();
            request.setDepartmentId("ENG");
            request.setTitle("Software Engineer");

            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePosition("UNKNOWN", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldPropagateExceptionWhenNewDepartmentIsNotFound() {
            PositionUpdateRequest request = new PositionUpdateRequest();
            request.setDepartmentId("UNKNOWN");
            request.setTitle("Software Engineer");

            when(repository.findById("SWE")).thenReturn(Optional.of(position));
            when(departmentService.getDepartmentById("UNKNOWN"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Department UNKNOWN not found"));

            assertThatThrownBy(() -> service.updatePosition("SWE", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class DeletePositionTests {

        @Test
        void shouldSoftDeletePositionWhenItExistsAndHasNoActiveEmployees() {
            when(repository.findById("SWE")).thenReturn(Optional.of(position));
            when(repository.countEmployeesByPositionId("SWE")).thenReturn(0L);

            service.deletePosition("SWE");

            assertThat(position.getDeletedAt()).isNotNull();
            verify(repository).save(position);
        }

        @Test
        void shouldThrowConflictWhenDeletingPositionWithActiveEmployees() {
            when(repository.findById("SWE")).thenReturn(Optional.of(position));
            when(repository.countEmployeesByPositionId("SWE")).thenReturn(2L);

            assertThatThrownBy(() -> service.deletePosition("SWE"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("Software Engineer");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenPositionToDeleteIsNotFound() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePosition("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class HasPositionsByDepartmentIdTests {

        @Test
        void shouldReturnTrueWhenPositionsExistForDepartment() {
            when(repository.existsByDepartmentId("ENG")).thenReturn(true);

            boolean result = service.hasPositionsByDepartmentId("ENG");

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNoPositionsExistForDepartment() {
            when(repository.existsByDepartmentId("ENG")).thenReturn(false);

            boolean result = service.hasPositionsByDepartmentId("ENG");

            assertThat(result).isFalse();
        }
    }
}