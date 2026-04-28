package com.rvce.scas.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class JwtPrincipal {
    private final UUID userId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
}
