package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.security.jwt.JwtService;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthResponse authenticate(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (InternalAuthenticationServiceException | BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userService.getUserByEmail(request.getEmail());
        String userRole = user.getRole().getName();

        // Validate that user can access the requested access type
        if (AccessType.ADMIN.equals(request.getAccessType()) && "EMPLOYEE".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid access for this account");
        }

        String accessToken = generateAccessToken(user.getEmail(), request.getAccessType());

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                request.getAccessType().name(),
                user.getRole().getName(),
                user.getEmployee().getId(),
                accessToken
        );
    }

    public String generateAccessToken(String email, AccessType accessType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accessType", accessType.name());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtService.generateAccessToken(claims, userDetails);
    }

    public String generateAccessToken(HttpServletRequest request) {
        String refreshToken = jwtService.getTokenFromCookie(request);

        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing refresh token");
        }

        String email = jwtService.extractUserEmail(refreshToken);
        String accessType = jwtService.extractAccessType(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token. Please reauthenticate");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("accessType", accessType);

        return jwtService.generateAccessToken(claims, userDetails);
    }

    public String generateRefreshToken(String email, AccessType accessType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accessType", accessType.name());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtService.generateRefreshToken(claims, userDetails);
    }

    // Cookie Management Abstraction
    public void addRefreshTokenCookie(String token, HttpServletResponse response) {
        jwtService.addTokenToCookie(token, response);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        jwtService.clearJwtCookie(response);
    }

}
