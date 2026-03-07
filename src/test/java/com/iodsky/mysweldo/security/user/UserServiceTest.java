package com.iodsky.mysweldo.security.user;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.role.RoleService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    @Mock
    private RoleService roleService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User savedUser;
    private Role role;
    private Employee employee;

    @BeforeEach
    void setUp() {
        role = new Role("ADMIN");
        employee = Employee.builder().id(1001L).build();
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .password("encoded-password")
                .role(role)
                .employee(employee)
                .build();
    }

    @Nested
    class LoadUserByUsernameTests {

        @Test
        void shouldReturnUserDetailsWhenUserExists() {
            when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));

            UserDetails result = service.loadUserByUsername("john@example.com");

            assertThat(result).isEqualTo(savedUser);
        }

        @Test
        void shouldThrow404WithUsernameInMessageWhenUserNotFound() {
            when(repository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("unknown@example.com");
                    });
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void shouldCreateUserWithEncodedPasswordWhenRequestIsValid() {
            UserRequest request = new UserRequest();
            request.setEmployeeId(1001L);
            request.setRole("ADMIN");
            request.setEmail("john@example.com");
            request.setPassword("plaintext");

            User mappedUser = User.builder().email("john@example.com").password("plaintext").build();

            when(userMapper.toEntity(request)).thenReturn(mappedUser);
            when(employeeService.getEmployeeById(1001L)).thenReturn(employee);
            when(roleService.getRoleByName("ADMIN")).thenReturn(role);
            when(passwordEncoder.encode("plaintext")).thenReturn("encoded-password");
            when(repository.save(any(User.class))).thenReturn(savedUser);

            User result = service.createUser(request);

            assertThat(result).isEqualTo(savedUser);
            assertThat(mappedUser.getPassword()).isEqualTo("encoded-password");
            assertThat(mappedUser.getEmployee()).isEqualTo(employee);
            assertThat(mappedUser.getRole()).isEqualTo(role);
        }

        @Test
        void shouldNotSaveUserWhenEmployeeNotFound() {
            UserRequest request = new UserRequest();
            request.setEmployeeId(9999L);
            request.setRole("ADMIN");
            request.setEmail("john@example.com");
            request.setPassword("plaintext");

            when(userMapper.toEntity(request)).thenReturn(User.builder().build());
            when(employeeService.getEmployeeById(9999L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee 9999 not found"));

            assertThatThrownBy(() -> service.createUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }

        @Test
        void shouldNotSaveUserWhenRoleNotFound() {
            UserRequest request = new UserRequest();
            request.setEmployeeId(1001L);
            request.setRole("NONEXISTENT");
            request.setEmail("john@example.com");
            request.setPassword("plaintext");

            when(userMapper.toEntity(request)).thenReturn(User.builder().build());
            when(employeeService.getEmployeeById(1001L)).thenReturn(employee);
            when(roleService.getRoleByName("NONEXISTENT"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Role NONEXISTENT not found"));

            assertThatThrownBy(() -> service.createUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class GetAllUsersTests {

        @Test
        void shouldReturnAllUsersWhenRoleNameIsNull() {
            Page<User> page = new PageImpl<>(List.of(savedUser));
            when(repository.findAll(any(Pageable.class))).thenReturn(page);

            Page<User> result = service.getAllUsers(0, 10, null);

            assertThat(result.getContent()).containsExactly(savedUser);
        }

        @Test
        void shouldReturnFilteredUsersWhenRoleNameIsProvided() {
            Page<User> page = new PageImpl<>(List.of(savedUser));
            when(repository.findAllByRole_Name(eq("ADMIN"), any(Pageable.class))).thenReturn(page);

            Page<User> result = service.getAllUsers(0, 10, "ADMIN");

            assertThat(result.getContent()).containsExactly(savedUser);
        }

        @Test
        void shouldReturnEmptyPageWhenNoUsersExist() {
            Page<User> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<User> result = service.getAllUsers(0, 10, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetAuthenticatedUserTests {

        @Test
        void shouldReturnUserWhenPrincipalIsUserInstance() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(savedUser);
            SecurityContextHolder.setContext(securityContext);

            User result = service.getAuthenticatedUser();

            assertThat(result).isEqualTo(savedUser);
        }

        @Test
        void shouldThrow401WhenPrincipalIsNotUserInstance() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            SecurityContextHolder.setContext(securityContext);

            assertThatThrownBy(() -> service.getAuthenticatedUser())
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void shouldReturnUserWhenEmailExists() {
            when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));

            User result = service.getUserByEmail("john@example.com");

            assertThat(result).isEqualTo(savedUser);
        }

        @Test
        void shouldThrow404WithEmailInMessageWhenEmailNotFound() {
            when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserByEmail("missing@example.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("missing@example.com");
                    });
        }
    }

    @Nested
    class UpdateUserRoleTests {

        @Test
        void shouldUpdateAndReturnUserWithNewRoleWhenUserAndRoleExist() {
            UUID userId = savedUser.getId();
            Role newRole = new Role("HR");

            when(repository.findById(userId)).thenReturn(Optional.of(savedUser));
            when(roleService.getRoleByName("HR")).thenReturn(newRole);
            when(repository.save(savedUser)).thenReturn(savedUser);

            User result = service.updateUserRole(userId, "HR");

            assertThat(result.getRole()).isEqualTo(newRole);
        }

        @Test
        void shouldThrow404WithUserIdInMessageWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateUserRole(unknownId, "HR"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(unknownId.toString());
                    });

            verify(repository, never()).save(any());
        }

        @Test
        void shouldNotSaveUserWhenRoleNotFound() {
            UUID userId = savedUser.getId();
            when(repository.findById(userId)).thenReturn(Optional.of(savedUser));
            when(roleService.getRoleByName("INVALID"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Role INVALID not found"));

            assertThatThrownBy(() -> service.updateUserRole(userId, "INVALID"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void shouldReturnUserWhenUserExists() {
            UUID userId = savedUser.getId();
            when(repository.findById(userId)).thenReturn(Optional.of(savedUser));

            User result = service.getUserById(userId);

            assertThat(result).isEqualTo(savedUser);
        }

        @Test
        void shouldThrow404WithUserIdInMessageWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserById(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(unknownId.toString());
                    });
        }
    }
}