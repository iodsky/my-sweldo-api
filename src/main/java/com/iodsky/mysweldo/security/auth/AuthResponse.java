package com.iodsky.mysweldo.security.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private String role;
    private Long employeeId;
}
