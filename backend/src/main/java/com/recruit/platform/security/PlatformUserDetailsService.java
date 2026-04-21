package com.recruit.platform.security;

import com.recruit.platform.common.UnauthorizedException;
import com.recruit.platform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(PlatformUserPrincipal::new)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
    }
}
