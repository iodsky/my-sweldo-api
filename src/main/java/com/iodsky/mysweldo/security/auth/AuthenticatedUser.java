package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.security.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatedUser {
    private UserDto user;
    private AccessType accessType;
}
