package com.iodsky.mysweldo.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthRequest {

    @NotNull
    @Email
    private String email;

    @NotNull
    private String password;

    @NotNull
    private AccessType accessType;
}
