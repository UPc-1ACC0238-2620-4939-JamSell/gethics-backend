package com.jamsell.gethics.iam.application.internal.commandservices;

import com.jamsell.gethics.iam.application.internal.outboundservices.hashing.HashingService;
import com.jamsell.gethics.iam.application.internal.outboundservices.mail.PasswordResetMailSender;
import com.jamsell.gethics.iam.application.internal.outboundservices.tokens.TokenService;
import com.jamsell.gethics.iam.domain.model.aggregates.PasswordResetToken;
import com.jamsell.gethics.iam.domain.model.aggregates.User;
import com.jamsell.gethics.iam.domain.model.commands.ForgotPasswordCommand;
import com.jamsell.gethics.iam.domain.model.commands.LoginCommand;
import com.jamsell.gethics.iam.domain.model.commands.RegisterUserCommand;
import com.jamsell.gethics.iam.domain.model.commands.ResetPasswordCommand;
import com.jamsell.gethics.iam.domain.model.valueobjects.ResetTokenCodec;
import com.jamsell.gethics.iam.domain.model.valueobjects.Role;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenRepository;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.ProfilePhotoRepository;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    private static final String RESET_URL = "http://localhost:4200/reset-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private ProfilePhotoRepository profilePhotoRepository;

    @Mock
    private HashingService hashingService;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordResetMailSender mailSender;

    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserCommandServiceImpl(
                userRepository,
                passwordResetTokenRepository,
                profilePhotoRepository,
                hashingService,
                tokenService,
                mailSender,
                30,
                RESET_URL
        );
    }

    @Test
    void register_withNewEmail_savesUserWithHashedPassword() {
        when(hashingService.encode("secreto123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new RegisterUserCommand("Ana Perez", "Ana@Gethics.com", "secreto123", Role.GANADERO));

        assertThat(result.isSuccess()).isTrue();
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@gethics.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.GANADERO);
    }

    @Test
    void register_withExistingEmail_returnsConflict() {
        when(userRepository.existsByEmail("ana@gethics.com")).thenReturn(true);

        var result = service.handle(new RegisterUserCommand("Ana Perez", "ana@gethics.com", "secreto123", Role.GANADERO));

        assertThat(errorOf(result).code()).isEqualTo("EMAIL_CONFLICT");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_returnsTokensAndUser() {
        var user = new User("Ana Perez", "ana@gethics.com", "hashed", Role.VETERINARIO);
        when(userRepository.findByEmail("ana@gethics.com")).thenReturn(Optional.of(user));
        when(hashingService.matches("secreto123", "hashed")).thenReturn(true);
        when(tokenService.generateAccessToken(user)).thenReturn("access");
        when(tokenService.generateRefreshToken(user)).thenReturn("refresh");
        when(tokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        var result = service.handle(new LoginCommand("ana@gethics.com", "secreto123"));

        var authenticated = valueOf(result);
        assertThat(authenticated.accessToken()).isEqualTo("access");
        assertThat(authenticated.refreshToken()).isEqualTo("refresh");
        assertThat(authenticated.expiresInSeconds()).isEqualTo(3600L);
        assertThat(authenticated.user().getRole()).isEqualTo(Role.VETERINARIO);
    }

    @Test
    void login_withWrongPassword_returnsGenericInvalidCredentials() {
        var user = new User("Ana Perez", "ana@gethics.com", "hashed", Role.GANADERO);
        when(userRepository.findByEmail("ana@gethics.com")).thenReturn(Optional.of(user));
        when(hashingService.matches("incorrecta", "hashed")).thenReturn(false);

        var result = service.handle(new LoginCommand("ana@gethics.com", "incorrecta"));

        assertThat(errorOf(result)).isEqualTo(ApplicationError.invalidCredentials());
    }

    @Test
    void login_withUnknownEmail_returnsSameErrorAsWrongPassword() {
        when(userRepository.findByEmail("nadie@gethics.com")).thenReturn(Optional.empty());

        var result = service.handle(new LoginCommand("nadie@gethics.com", "secreto123"));

        assertThat(errorOf(result)).isEqualTo(ApplicationError.invalidCredentials());
    }

    @Test
    void forgotPassword_withUnknownEmail_returnsNotFoundAndSendsNothing() {
        when(userRepository.findByEmail("nadie@gethics.com")).thenReturn(Optional.empty());

        var result = service.handle(new ForgotPasswordCommand("nadie@gethics.com"));

        assertThat(errorOf(result).code()).isEqualTo("EMAIL_NOT_FOUND");
        verify(mailSender, never()).sendPasswordResetLink(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void forgotPassword_withKnownEmail_storesOnlyTheHashAndSendsLink() {
        var user = userWithId(7L);
        when(userRepository.findByEmail("ana@gethics.com")).thenReturn(Optional.of(user));

        var result = service.handle(new ForgotPasswordCommand("ana@gethics.com"));

        assertThat(result.isSuccess()).isTrue();
        verify(passwordResetTokenRepository).deleteByUserId(7L);
        var tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        var linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendPasswordResetLink(eq("ana@gethics.com"), eq("Ana Perez"), linkCaptor.capture(), eq(30L));
        assertThat(linkCaptor.getValue()).startsWith(RESET_URL + "?token=");
        var rawToken = linkCaptor.getValue().substring((RESET_URL + "?token=").length());
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(ResetTokenCodec.hash(rawToken));
        assertThat(tokenCaptor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void resetPassword_withExpiredToken_isRejected() {
        var expired = new PasswordResetToken(7L, ResetTokenCodec.hash("abc"), Instant.now().minusSeconds(60));
        when(passwordResetTokenRepository.findByTokenHash(ResetTokenCodec.hash("abc"))).thenReturn(Optional.of(expired));

        var result = service.handle(new ResetPasswordCommand("abc", "nuevaClave123"));

        assertThat(errorOf(result).code()).isEqualTo("VALIDATION_ERROR");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_withUsedToken_isRejected() {
        var used = new PasswordResetToken(7L, ResetTokenCodec.hash("abc"), Instant.now().plusSeconds(600));
        used.markUsed(Instant.now());
        when(passwordResetTokenRepository.findByTokenHash(ResetTokenCodec.hash("abc"))).thenReturn(Optional.of(used));

        var result = service.handle(new ResetPasswordCommand("abc", "nuevaClave123"));

        assertThat(errorOf(result).code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void resetPassword_withValidToken_changesPasswordAndConsumesToken() {
        var token = new PasswordResetToken(7L, ResetTokenCodec.hash("abc"), Instant.now().plusSeconds(600));
        var user = userWithId(7L);
        when(passwordResetTokenRepository.findByTokenHash(ResetTokenCodec.hash("abc"))).thenReturn(Optional.of(token));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(hashingService.encode("nuevaClave123")).thenReturn("newHash");

        var result = service.handle(new ResetPasswordCommand("abc", "nuevaClave123"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    private static User userWithId(Long id) {
        var user = new User("Ana Perez", "ana@gethics.com", "oldHash", Role.GANADERO);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static <T> T valueOf(Result<T, ApplicationError> result) {
        if (result instanceof Result.Success<T, ApplicationError> success) {
            return success.value();
        }
        throw new AssertionError("Se esperaba un resultado exitoso");
    }

    private static ApplicationError errorOf(Result<?, ApplicationError> result) {
        if (result instanceof Result.Failure<?, ApplicationError> failure) {
            return failure.error();
        }
        throw new AssertionError("Se esperaba un fallo");
    }
}
