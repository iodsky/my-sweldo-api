package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import com.iodsky.mysweldo.security.jwt.JwtCookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService service;
    private final JwtCookieProvider jwtCookieProvider;

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT token", description = "Authenticate with email and password to receive a JWT token in a secure HTTP-only cookie for accessing protected endpoints")
    @SecurityRequirements()
    public ApiResponse<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthResponse authResponse = service.authenticate(request);

        String token = service.generateToken(authResponse.getEmail());
        jwtCookieProvider.addJwtCookie(token, response);

        return ResponseFactory.success("Authentication successful", authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and clear JWT token", description = "Logout from the system and clear the JWT token cookie")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        jwtCookieProvider.clearJwtCookie(response);
        return ResponseFactory.success("Logout success", null);
    }

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user", description = "Retrieve the current authenticated user's information from the JWT token")
    public ApiResponse<AuthResponse> getAuthenticatedUser(HttpServletRequest request) {
        AuthResponse authResponse = service.getAuthenticatedUser(request);
        return ResponseFactory.success("Authentication success", authResponse);
    }

}
