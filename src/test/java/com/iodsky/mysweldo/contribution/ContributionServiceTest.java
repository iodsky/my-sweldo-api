package com.iodsky.mysweldo.contribution;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @InjectMocks
    private ContributionService service;

    @Mock
    private ContributionRepository repository;

    @Nested
    class CreateContributionTests {

        @Test
        void shouldCreateAndReturnContributionWhenCodeDoesNotExist() {
            ContributionRequest request = ContributionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            Contribution saved = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(false);
            when(repository.save(any(Contribution.class))).thenReturn(saved);

            Contribution result = service.createContribution(request);

            assertEquals("SSS", result.getCode());
            assertEquals("Social Security System", result.getDescription());
        }

        @Test
        void shouldThrowConflictWhenContributionWithSameCodeAlreadyExists() {
            ContributionRequest request = ContributionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(true);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.createContribution(request)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
    }

    @Nested
    class GetAllContributionsTests {

        @Test
        void shouldReturnPaginatedContributionsForValidPageAndLimit() {
            List<Contribution> contributions = List.of(
                    Contribution.builder().code("SSS").description("Social Security System").build(),
                    Contribution.builder().code("PHIC").description("PhilHealth").build()
            );
            Page<Contribution> page = new PageImpl<>(contributions, PageRequest.of(0, 10), 2);

            when(repository.findAll(PageRequest.of(0, 10))).thenReturn(page);

            Page<Contribution> result = service.getAllContributions(0, 10);

            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getContent().size());
        }

        @Test
        void shouldReturnEmptyPageWhenNoContributionsExist() {
            Page<Contribution> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(repository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

            Page<Contribution> result = service.getAllContributions(0, 10);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetContributionByCodeTests {

        @Test
        void shouldReturnContributionWhenItExistsAndIsNotDeleted() {
            Contribution contribution = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findById("SSS")).thenReturn(Optional.of(contribution));

            Contribution result = service.getContributionByCode("SSS");

            assertEquals("SSS", result.getCode());
            assertEquals("Social Security System", result.getDescription());
        }

        @Test
        void shouldThrowNotFoundWhenContributionDoesNotExist() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getContributionByCode("UNKNOWN")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenContributionIsSoftDeleted() {
            Contribution deleted = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();
            deleted.setDeletedAt(Instant.now());

            when(repository.findById("SSS")).thenReturn(Optional.of(deleted));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getContributionByCode("SSS")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class UpdateContributionTests {

        @Test
        void shouldUpdateAndReturnContributionWithNewDescriptionWhenItExists() {
            Contribution existing = Contribution.builder()
                    .code("SSS")
                    .description("Old Description")
                    .build();

            ContributionRequest request = ContributionRequest.builder()
                    .code("SSS")
                    .description("Updated Description")
                    .build();

            when(repository.findById("SSS")).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            Contribution result = service.updateContribution("SSS", request);

            assertEquals("Updated Description", result.getDescription());
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentContribution() {
            ContributionRequest request = ContributionRequest.builder()
                    .code("UNKNOWN")
                    .description("Some Description")
                    .build();

            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updateContribution("UNKNOWN", request)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class DeleteContributionTests {

        @Test
        void shouldSoftDeleteContributionBySettingDeletedAtWhenItExists() {
            Contribution contribution = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findById("SSS")).thenReturn(Optional.of(contribution));

            service.deleteContribution("SSS");

            assertNotNull(contribution.getDeletedAt());
            verify(repository).save(contribution);
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentContribution() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.deleteContribution("UNKNOWN")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }
}

