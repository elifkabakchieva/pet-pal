package com.petpal.identity;

import com.petpal.common.error.ConflictException;
import com.petpal.common.error.InvalidCredentialsException;
import com.petpal.common.error.ResourceNotFoundException;
import com.petpal.identity.JwtTokenService.IssuedToken;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final ApplicationEventPublisher events;
    private final String dummyPasswordHash;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens,
            ApplicationEventPublisher events) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.events = events;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = User.register(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                blankToNull(request.phone()),
                request.city().trim(),
                request.accountType().toRole());
        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw emailTaken();
        }
        events.publishEvent(new UserRegisteredEvent(user.getId(), user.getRole(), user.fullName(), user.getCity()));
        return authResponseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> candidate = users.findByEmail(normaliseEmail(request.email()));
        if (candidate.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new InvalidCredentialsException();
        }
        User user = candidate.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }
        return authResponseFor(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "This account no longer exists."));
    }

    private AuthResponse authResponseFor(User user) {
        IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), UserResponse.from(user));
    }

    private static ConflictException emailTaken() {
        return new ConflictException("EMAIL_TAKEN", "An account with this e-mail already exists. Try signing in.");
    }

    static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
