package com.recruit.platform.security;

import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.user.User;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class PlatformUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String displayName;
    private final String departmentName;
    private final Set<RoleType> roles;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public PlatformUserPrincipal(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.departmentName = user.getDepartment() == null ? null : user.getDepartment().getName();
        this.roles = user.getRoles();
        this.authorities = user.getRoles().stream()
                .map(roleType -> new SimpleGrantedAuthority("ROLE_" + roleType.name()))
                .collect(Collectors.toSet());
        this.enabled = user.isEnabled();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
