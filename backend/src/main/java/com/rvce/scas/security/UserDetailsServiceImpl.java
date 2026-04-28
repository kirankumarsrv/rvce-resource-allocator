package com.rvce.scas.security;

import com.rvce.scas.entity.User;
import com.rvce.scas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Auth attempt for unknown email: {}", email);
                    return new UsernameNotFoundException("No user found with email: " + email);
                });

        return new ScasPrincipal(user, buildAuthorities(user));
    }

    private Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .flatMap(role -> {
                    Set<GrantedAuthority> roleAuthorities = new HashSet<>();
                    roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    role.getRolePermissions().stream()
                            .map(rp -> rp.getPermission())
                            .map(perm -> perm.getResource().toUpperCase() + "_" + perm.getAction().toUpperCase())
                            .map(SimpleGrantedAuthority::new)
                            .forEach(roleAuthorities::add);
                    return roleAuthorities.stream();
                })
                .collect(Collectors.toSet());

        log.debug("Loaded {} authorities for user {}", authorities.size(), user.getEmail());
        return authorities;
    }
}
