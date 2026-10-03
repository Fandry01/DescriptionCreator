package com.descriptioncreator.backend.auth;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository repository;

    public AppUserDetailsService(AppUserRepository repository) { this.repository = repository; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUserEntity user = repository.findByEmailIgnoreCase(AppUserEntity.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities("ROLE_USER")
                .build();
    }
}
