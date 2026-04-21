package com.recruit.platform.auth;

import com.recruit.platform.common.UnauthorizedException;
import com.recruit.platform.config.AppAuthProperties;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.security.JwtService;
import com.recruit.platform.security.PlatformUserPrincipal;
import com.recruit.platform.security.PlatformUserDetailsService;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PlatformUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AppAuthProperties authProperties;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        PlatformUserPrincipal principal =
                (PlatformUserPrincipal) userDetailsService.loadUserByUsername(request.username());
        return issueTokens(principal);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));
        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }
        PlatformUserPrincipal principal =
                (PlatformUserPrincipal) userDetailsService.loadUserByUsername(refreshToken.getUser().getUsername());
        return issueTokens(principal);
    }

    public MeResponse me() {
        User user = currentUserService.getRequiredUser();
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }

    private AuthResponse issueTokens(PlatformUserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshTokenValue = jwtService.generateRefreshToken(principal);
        refreshTokenRepository.save(buildRefreshToken(principal.getUserId(), refreshTokenValue));
        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                OffsetDateTime.now().plus(authProperties.accessTokenExpiry()),
                principal.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                principal.getDisplayName()
        );
    }

    private RefreshToken buildRefreshToken(Long userId, String token) {
        RefreshToken refreshToken = new RefreshToken();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(OffsetDateTime.now().plus(authProperties.refreshTokenExpiry()));
        return refreshToken;
    }
}
