package com.iodsky.mysweldo.security.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccessToken {
    private String token;
}
