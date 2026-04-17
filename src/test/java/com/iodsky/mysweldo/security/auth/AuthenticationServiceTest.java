package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.security.jwt.JwtService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserDto;
import com.iodsky.mysweldo.security.user.UserMapper;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    private AuthRequest validLoginRequest;
    private User validUser;
    private UserDto validUserDto;

    @BeforeEach
    void setUp() {
        Employee employee = Employee.builder().id(10000L).build();
        Role employeeRole = Role.builder().name("EMPLOYEE").build();

        validLoginRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("secret123")
                .accessType(AccessType.EMPLOYEE)
                .build();

        validUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .email("john@example.com")
                .role(employeeRole)
                .build();

        validUserDto = UserDto.builder()
                .id(validUser.getId())
                .email("john@example.com")
                .employeeId(10000L)
                .role("EMPLOYEE")
                .build();
    }

    @Nested
    class AuthenticateTests {

        @Test
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("john@example.com")
                    .password("encoded")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);
            when(jwtService.generateAccessToken(any(), any()))
                    .thenReturn("mocked.access.token");
            when(userMapper.toDto(validUser)).thenReturn(validUserDto);

            AuthSession response = authenticationService.authenticate(validLoginRequest);

            assertNotNull(response);
            assertEquals("john@example.com", response.getUser().getEmail());
            assertEquals("EMPLOYEE", response.getUser().getRole());
            assertEquals("mocked.access.token", response.getToken());
        }

        @Test
        void shouldPropagateBadCredentialsException() {
            AuthRequest invalidLoginRequest = AuthRequest.builder()
                    .email("john@example.com")
                    .password("wrongpassword")
                    .accessType(AccessType.EMPLOYEE)
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(
                    BadCredentialsException.class,
                    () -> authenticationService.authenticate(invalidLoginRequest)
            );
        }

        @Test
        void shouldReturnResponseBelongingToAuthenticatedUser() {
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("john@example.com")
                    .password("encoded")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);
            when(jwtService.generateAccessToken(any(), any()))
                    .thenReturn("mocked.access.token");
            when(userMapper.toDto(validUser)).thenReturn(validUserDto);

            AuthSession response = authenticationService.authenticate(validLoginRequest);

            assertEquals("john@example.com", response.getUser().getEmail());
        }

        @Test
        void shouldThrowForbiddenWhenAdminAccessDeniedForEmployee() {
            AuthRequest adminLoginRequest = AuthRequest.builder()
                    .email("john@example.com")
                    .password("secret123")
                    .accessType(AccessType.ADMIN)
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> authenticationService.authenticate(adminLoginRequest)
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }
}
