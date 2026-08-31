package com.elmeftouhi.expense.security;

import com.elmeftouhi.expense.user.AppUser;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Set<GrantedAuthority> authorities;

    public AppUserDetails(AppUser user) {
        this.id = user.getId();
        this.username = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}

