package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.security.jwt.JwtCookieProvider;
import com.iodsky.mysweldo.security.jwt.JwtUtil;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtCookieProvider jwtCookieProvider;

    private AuthRequest validLoginRequest;
    private User validUser;
    private Role employeeRole;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(10000L).build();
        employeeRole = Role.builder().name("EMPLOYEE").build();

        validLoginRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("secret123")
                .role("EMPLOYEE")
                .build();

        validUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .email("john@example.com")
                .role(employeeRole)
                .build();
    }

    @Nested
    class AuthenticateTests {

        @Test
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            AuthResponse response = authenticationService.authenticate(validLoginRequest);

            assertNotNull(response);
            assertEquals("john@example.com", response.getEmail());
            assertEquals("EMPLOYEE", response.getRole());
        }

        @Test
        void shouldPropagateBadCredentialsException() {
            AuthRequest invalidLoginRequest = AuthRequest.builder()
                    .email("john@example.com")
                    .password("wrongpassword")
                    .role("EMPLOYEE")
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
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            AuthResponse response = authenticationService.authenticate(validLoginRequest);

            assertEquals("john@example.com", response.getEmail());
        }

        @Test
        void shouldThrowForbiddenWhenAdminAccessDeniedForEmployee() {
            AuthRequest adminLoginRequest = AuthRequest.builder()
                    .email("john@example.com")
                    .password("secret123")
                    .role("ADMIN")
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

    @Nested
    class GenerateTokenTests {

        @Test
        void shouldGenerateTokenForValidEmail() {
            String email = "john@example.com";
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("encoded")
                    .build();

            when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
            when(jwtUtil.generateToken(userDetails)).thenReturn("mocked.jwt.token");

            String token = authenticationService.generateToken(email);

            assertNotNull(token);
            assertEquals("mocked.jwt.token", token);
        }
    }

    @Nested
    class GetAuthenticatedUserTests {

        private HttpServletRequest mockRequest;
        private String validToken;

        @BeforeEach
        void setUp() {
            mockRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
            validToken = "valid.jwt.token";
        }

        @Test
        void shouldReturnAuthResponseWhenTokenIsValid() {
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("john@example.com")
                    .password("encoded")
                    .build();

            when(jwtCookieProvider.getTokenFromCookie(mockRequest)).thenReturn(validToken);
            when(jwtUtil.extractUserEmail(validToken)).thenReturn("john@example.com");
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtUtil.isTokenValid(any(), any())).thenReturn(true);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            AuthResponse response = authenticationService.getAuthenticatedUser(mockRequest);

            assertNotNull(response);
            assertEquals("john@example.com", response.getEmail());
            assertEquals("EMPLOYEE", response.getRole());
        }

        @Test
        void shouldThrowUnauthorizedExceptionWhenTokenIsInvalid() {
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("john@example.com")
                    .password("encoded")
                    .build();

            when(jwtCookieProvider.getTokenFromCookie(mockRequest)).thenReturn(validToken);
            when(jwtUtil.extractUserEmail(validToken)).thenReturn("john@example.com");
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtUtil.isTokenValid(any(), any())).thenReturn(false);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> authenticationService.getAuthenticatedUser(mockRequest)
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        }

        @Test
        void shouldThrowUnauthorizedExceptionWhenTokenIsNull() {
            when(jwtCookieProvider.getTokenFromCookie(mockRequest)).thenReturn(null);
            when(jwtUtil.extractUserEmail(null)).thenThrow(new IllegalArgumentException("Token cannot be null"));

            assertThrows(
                    Exception.class,
                    () -> authenticationService.getAuthenticatedUser(mockRequest)
            );
        }

        @Test
        void shouldReturnCorrectUserDataFromDatabase() {
            User adminUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(Employee.builder().id(20000L).build())
                    .email("admin@example.com")
                    .role(Role.builder().name("ADMIN").build())
                    .build();

            UserDetails adminUserDetails = org.springframework.security.core.userdetails.User
                    .withUsername("admin@example.com")
                    .password("encoded")
                    .build();

            when(jwtCookieProvider.getTokenFromCookie(mockRequest)).thenReturn(validToken);
            when(jwtUtil.extractUserEmail(validToken)).thenReturn("admin@example.com");
            when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(adminUserDetails);
            when(jwtUtil.isTokenValid(any(), any())).thenReturn(true);
            when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);

            AuthResponse response = authenticationService.getAuthenticatedUser(mockRequest);

            assertEquals("admin@example.com", response.getEmail());
            assertEquals("ADMIN", response.getRole());
        }
    }
}

