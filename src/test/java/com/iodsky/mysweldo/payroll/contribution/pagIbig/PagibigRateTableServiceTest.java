package com.iodsky.mysweldo.payroll.contribution.pagIbig;

import com.iodsky.mysweldo.pagIbig.PagibigRateTable;
import com.iodsky.mysweldo.pagIbig.PagibigRateTableRepository;
import com.iodsky.mysweldo.pagIbig.PagibigRateTableRequest;
import com.iodsky.mysweldo.pagIbig.PagibigRateTableService;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagibigRateTableServiceTest {

    @InjectMocks
    private PagibigRateTableService service;

    @Mock
    private PagibigRateTableRepository pagibigRateTableRepository;

    private PagibigRateTable rateTable;
    private PagibigRateTableRequest request;

    @BeforeEach
    void setUp() {
        rateTable = PagibigRateTable.builder()
                .id(UUID.randomUUID())
                .employeeRate(new BigDecimal("0.0200"))
                .employerRate(new BigDecimal("0.0200"))
                .lowIncomeThreshold(new BigDecimal("1500.00"))
                .lowIncomeEmployeeRate(new BigDecimal("0.0100"))
                .maxSalaryCap(new BigDecimal("5000.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();

        request = PagibigRateTableRequest.builder()
                .employeeRate(new BigDecimal("0.0200"))
                .employerRate(new BigDecimal("0.0200"))
                .lowIncomeThreshold(new BigDecimal("1500.00"))
                .lowIncomeEmployeeRate(new BigDecimal("0.0100"))
                .maxSalaryCap(new BigDecimal("5000.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Nested
    class CreatePagibigRateTableTests {

        @Test
        void shouldCreateRateTableWhenNoExistingRecordForEffectiveDate() {
            when(pagibigRateTableRepository.findLatestByEffectiveDate(request.getEffectiveDate()))
                    .thenReturn(Optional.empty());
            when(pagibigRateTableRepository.save(any(PagibigRateTable.class))).thenReturn(rateTable);

            PagibigRateTable result = service.createPagibigRateTable(request);

            assertThat(result).isNotNull();
            assertThat(result.getEmployeeRate()).isEqualTo(request.getEmployeeRate());
            assertThat(result.getEmployerRate()).isEqualTo(request.getEmployerRate());
            assertThat(result.getEffectiveDate()).isEqualTo(request.getEffectiveDate());
        }

        @Test
        void shouldThrowConflictWhenRateTableAlreadyExistsForEffectiveDate() {
            when(pagibigRateTableRepository.findLatestByEffectiveDate(request.getEffectiveDate()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.createPagibigRateTable(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains(request.getEffectiveDate().toString());
                    });

            verify(pagibigRateTableRepository, never()).save(any());
        }
    }

    @Nested
    class GetAllPagibigRateTablesTests {

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedRateTablesWhenNoEffectiveDateFilterProvided() {
            Page<PagibigRateTable> expectedPage = new PageImpl<>(List.of(rateTable));
            when(pagibigRateTableRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<PagibigRateTable> result = service.getAllPagibigRateTables(0, 10, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(rateTable);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedRateTablesFilteredByEffectiveDateWhenDateProvided() {
            Page<PagibigRateTable> expectedPage = new PageImpl<>(List.of(rateTable));
            when(pagibigRateTableRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<PagibigRateTable> result = service.getAllPagibigRateTables(0, 10, LocalDate.of(2024, 6, 1));

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnEmptyPageWhenNoRateTablesExist() {
            Page<PagibigRateTable> emptyPage = new PageImpl<>(List.of());
            when(pagibigRateTableRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<PagibigRateTable> result = service.getAllPagibigRateTables(0, 10, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetPagibigRateTableByIdTests {

        @Test
        void shouldReturnRateTableWhenItExistsAndIsNotDeleted() {
            when(pagibigRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            PagibigRateTable result = service.getPagibigRateTableById(rateTable.getId());

            assertThat(result).isEqualTo(rateTable);
        }

        @Test
        void shouldThrowNotFoundWhenRateTableDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(pagibigRateTableRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPagibigRateTableById(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void shouldThrowNotFoundWhenRateTableIsSoftDeleted() {
            rateTable.setDeletedAt(Instant.now());
            when(pagibigRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.getPagibigRateTableById(rateTable.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    class GetLatestPagibigRateTableTests {

        @Test
        void shouldReturnLatestRateTableForGivenDate() {
            LocalDate date = LocalDate.of(2024, 6, 1);
            when(pagibigRateTableRepository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.of(rateTable));

            PagibigRateTable result = service.getLatestPagibigRateTable(date);

            assertThat(result).isEqualTo(rateTable);
        }

        @Test
        void shouldThrowNotFoundWhenNoRateTableExistsForGivenDate() {
            LocalDate date = LocalDate.of(2020, 1, 1);
            when(pagibigRateTableRepository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLatestPagibigRateTable(date))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(date.toString());
                    });
        }
    }

    @Nested
    class UpdatePagibigRateTableTests {

        @Test
        void shouldUpdateAllFieldsAndReturnUpdatedRateTableWhenExists() {
            PagibigRateTableRequest updateRequest = PagibigRateTableRequest.builder()
                    .employeeRate(new BigDecimal("0.0300"))
                    .employerRate(new BigDecimal("0.0300"))
                    .lowIncomeThreshold(new BigDecimal("2000.00"))
                    .lowIncomeEmployeeRate(new BigDecimal("0.0200"))
                    .maxSalaryCap(new BigDecimal("8000.00"))
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(pagibigRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));
            when(pagibigRateTableRepository.save(any(PagibigRateTable.class))).thenAnswer(inv -> inv.getArgument(0));

            PagibigRateTable result = service.updatePagibigRateTable(rateTable.getId(), updateRequest);

            assertThat(result.getEmployeeRate()).isEqualTo(updateRequest.getEmployeeRate());
            assertThat(result.getEmployerRate()).isEqualTo(updateRequest.getEmployerRate());
            assertThat(result.getLowIncomeThreshold()).isEqualTo(updateRequest.getLowIncomeThreshold());
            assertThat(result.getLowIncomeEmployeeRate()).isEqualTo(updateRequest.getLowIncomeEmployeeRate());
            assertThat(result.getMaxSalaryCap()).isEqualTo(updateRequest.getMaxSalaryCap());
            assertThat(result.getEffectiveDate()).isEqualTo(updateRequest.getEffectiveDate());
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentRateTable() {
            UUID unknownId = UUID.randomUUID();
            when(pagibigRateTableRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePagibigRateTable(unknownId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingSoftDeletedRateTable() {
            rateTable.setDeletedAt(Instant.now());
            when(pagibigRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.updatePagibigRateTable(rateTable.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class DeletePagibigRateTableTests {

        @Test
        void shouldSoftDeleteRateTableBySettingDeletedAt() {
            when(pagibigRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));
            when(pagibigRateTableRepository.save(any(PagibigRateTable.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deletePagibigRateTable(rateTable.getId());

            assertThat(rateTable.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentRateTable() {
            UUID unknownId = UUID.randomUUID();
            when(pagibigRateTableRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePagibigRateTable(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowNotFoundWhenDeletingAlreadySoftDeletedRateTable() {
            rateTable.setDeletedAt(Instant.now());
            when(pagibigRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.deletePagibigRateTable(rateTable.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}