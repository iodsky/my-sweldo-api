package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.security.jwt.JwtUtil;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

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
    private JwtUtil jwtUtil;

    @Nested
    class AuthenticateTests {

        @Test
        void shouldReturnTokenWhenCredentialsAreValid() {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("secret123")
                    .build();

            User user = User.builder()
                    .email("john@example.com")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(user);
            when(jwtUtil.generateToken(user)).thenReturn("mocked.jwt.token");

            String token = authenticationService.authenticate(loginRequest);

            assertNotNull(token);
            assertEquals("mocked.jwt.token", token);
        }

        @Test
        void shouldThrow404WhenUserNotFoundDuringAuthentication() {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("ghost@example.com")
                    .password("secret123")
                    .build();

            String exceptionMessage = "404 NOT_FOUND \"User ghost@example.com not found\"";
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new InternalAuthenticationServiceException(exceptionMessage));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authenticationService.authenticate(loginRequest)
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("User ghost@example.com not found", ex.getReason());
        }

        @Test
        void shouldPropagateExceptionWhenInternalErrorIsNot404() {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("wrongpassword")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new InternalAuthenticationServiceException("500 Internal Server Error"));

            assertDoesNotThrow(() -> authenticationService.authenticate(loginRequest));
        }

        @Test
        void shouldPropagateBadCredentialsException() {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("wrongpassword")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(
                    BadCredentialsException.class,
                    () -> authenticationService.authenticate(loginRequest)
            );
        }

        @Test
        void shouldReturnTokenBelongingToAuthenticatedUser() {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("secret123")
                    .build();

            User user = User.builder()
                    .email("john@example.com")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(user);
            when(jwtUtil.generateToken(user)).thenReturn("user.specific.token");

            String token = authenticationService.authenticate(loginRequest);

            assertEquals("user.specific.token", token);
        }
    }
}

