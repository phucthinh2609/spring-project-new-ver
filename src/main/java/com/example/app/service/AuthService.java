package com.example.app.service;

import com.example.app.dto.AuthDto;
import com.example.app.dto.UserDto;
import com.example.app.entity.User;
import com.example.app.exception.ConflictException;
import com.example.app.repository.UserRepository;
import com.example.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .roles(Set.of("USER"))
                .build();
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.login(), req.password()));

        User user = userRepository.findByUsernameOrEmail(req.login())
                .orElseThrow();

        return buildAuthResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthDto.AuthResponse buildAuthResponse(User user) {
        UserDetails ud = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r))
                        .toList())
                .build();

        String accessToken  = jwtService.generateAccessToken(ud);
        String refreshToken = jwtService.generateRefreshToken(ud);

        UserDto userDto = new UserDto(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRoles(), user.getCreatedAt());

        return AuthDto.AuthResponse.of(accessToken, refreshToken, 86400L, userDto);
    }
}
