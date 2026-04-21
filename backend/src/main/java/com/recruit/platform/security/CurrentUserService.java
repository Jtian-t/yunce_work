package com.recruit.platform.security;

import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.UnauthorizedException;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public PlatformUserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PlatformUserPrincipal principal)) {
            throw new UnauthorizedException("Login required");
        }
        return principal;
    }

    public User getRequiredUser() {
        PlatformUserPrincipal principal = getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public boolean hasAnyRole(RoleType... roles) {
        Set<RoleType> userRoles = getPrincipal().getRoles();
        return Arrays.stream(roles).anyMatch(userRoles::contains);
    }

    public void requireAnyRole(RoleType... roles) {
        if (!hasAnyRole(roles)) {
            throw new ForbiddenException("Insufficient role to perform this action");
        }
    }
}
