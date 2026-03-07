package com.iodsky.mysweldo.security.role;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService service;

    @Mock
    private RoleRepository repository;

    @Nested
    class CreateRoleTests {

        @Test
        void shouldCreateAndReturnRoleWhenNameDoesNotExist() {
            RoleRequest request = new RoleRequest();
            request.setName("ADMIN");
            request.setDescription("Administrator role");

            Role savedRole = Role.builder().name("ADMIN").description("Administrator role").build();

            when(repository.existsByName("ADMIN")).thenReturn(false);
            when(repository.save(any(Role.class))).thenReturn(savedRole);

            Role result = service.createRole(request);

            assertEquals("ADMIN", result.getName());
            assertEquals("Administrator role", result.getDescription());
        }

        @Test
        void shouldThrowConflictWhenRoleNameAlreadyExists() {
            RoleRequest request = new RoleRequest();
            request.setName("ADMIN");

            when(repository.existsByName("ADMIN")).thenReturn(true);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.createRole(request)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
    }

    @Nested
    class GetRoleByIdTests {

        @Test
        void shouldReturnRoleWhenIdExists() {
            Role role = Role.builder().name("ADMIN").build();
            role.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(role));

            Role result = service.getRoleById(1L);

            assertEquals(1L, result.getId());
            assertEquals("ADMIN", result.getName());
        }

        @Test
        void shouldThrowNotFoundWhenIdDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getRoleById(99L)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class GetRoleByNameTests {

        @Test
        void shouldReturnRoleWhenNameExists() {
            Role role = Role.builder().name("USER").build();

            when(repository.findByName("USER")).thenReturn(Optional.of(role));

            Role result = service.getRoleByName("USER");

            assertEquals("USER", result.getName());
        }

        @Test
        void shouldThrowNotFoundWhenNameDoesNotExist() {
            when(repository.findByName("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getRoleByName("UNKNOWN")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class GetAllRolesTests {

        @Test
        void shouldReturnPaginatedRolesForGivenPageAndLimit() {
            List<Role> roles = List.of(
                    Role.builder().name("ADMIN").build(),
                    Role.builder().name("USER").build()
            );
            Page<Role> page = new PageImpl<>(roles, PageRequest.of(0, 10), roles.size());

            when(repository.findAll(PageRequest.of(0, 10))).thenReturn(page);

            Page<Role> result = service.getAllRoles(0, 10);

            assertEquals(2, result.getTotalElements());
        }

        @Test
        void shouldReturnEmptyPageWhenNoRolesExist() {
            Page<Role> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(repository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

            Page<Role> result = service.getAllRoles(0, 10);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class UpdateRoleTests {

        @Test
        void shouldUpdateBothNameAndDescriptionWhenBothProvided() {
            Role existingRole = Role.builder().name("OLD_NAME").description("Old desc").build();
            existingRole.setId(1L);

            RoleRequest request = new RoleRequest();
            request.setName("NEW_NAME");
            request.setDescription("New desc");

            when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.updateRole(1L, request);

            assertEquals("NEW_NAME", result.getName());
            assertEquals("New desc", result.getDescription());
        }

        @Test
        void shouldUpdateOnlyNameWhenDescriptionIsNull() {
            Role existingRole = Role.builder().name("OLD_NAME").description("Existing desc").build();
            existingRole.setId(1L);

            RoleRequest request = new RoleRequest();
            request.setName("NEW_NAME");

            when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.updateRole(1L, request);

            assertEquals("NEW_NAME", result.getName());
            assertEquals("Existing desc", result.getDescription());
        }

        @Test
        void shouldUpdateOnlyDescriptionWhenNameIsNull() {
            Role existingRole = Role.builder().name("ADMIN").description("Old desc").build();
            existingRole.setId(1L);

            RoleRequest request = new RoleRequest();
            request.setDescription("New desc");

            when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.updateRole(1L, request);

            assertEquals("ADMIN", result.getName());
            assertEquals("New desc", result.getDescription());
        }

        @Test
        void shouldThrowNotFoundWhenRoleToUpdateDoesNotExist() {
            RoleRequest request = new RoleRequest();
            request.setName("NEW_NAME");

            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updateRole(99L, request)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class DeleteRoleTests {

        @Test
        void shouldSoftDeleteRoleBySettingDeletedAtWhenRoleExistsAndIsNotInUse() {
            Role role = Role.builder().name("ADMIN").build();
            role.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(role));
            when(repository.isRoleUsedById(1L)).thenReturn(false);
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteRole(1L);

            assertNotNull(role.getDeletedAt());
        }

        @Test
        void shouldThrowConflictWhenRoleIsStillAssignedToUsers() {
            Role role = Role.builder().name("ADMIN").build();
            role.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(role));
            when(repository.isRoleUsedById(1L)).thenReturn(true);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.deleteRole(1L)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenRoleToDeleteDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.deleteRole(99L)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }
}