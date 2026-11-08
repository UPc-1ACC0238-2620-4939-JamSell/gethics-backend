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
import com.jamsell.gethics.iam.domain.model.commands.UpdateProfileCommand;
import com.jamsell.gethics.iam.domain.model.commands.UpdateProfilePhotoCommand;
import com.jamsell.gethics.iam.domain.model.entities.ProfilePhoto;
import com.jamsell.gethics.iam.domain.model.valueobjects.ResetTokenCodec;
import com.jamsell.gethics.iam.domain.services.UserCommandService;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenRepository;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.ProfilePhotoRepository;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.application.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ProfilePhotoRepository profilePhotoRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final PasswordResetMailSender passwordResetMailSender;
    private final long resetExpirationMinutes;
    private final String resetPasswordUrl;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            ProfilePhotoRepository profilePhotoRepository,
            HashingService hashingService,
            TokenService tokenService,
            PasswordResetMailSender passwordResetMailSender,
            @Value("${app.password-reset.expiration-minutes}") long resetExpirationMinutes,
            @Value("${app.frontend.reset-password-url}") String resetPasswordUrl
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.profilePhotoRepository = profilePhotoRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.passwordResetMailSender = passwordResetMailSender;
        this.resetExpirationMinutes = resetExpirationMinutes;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Override
    public Result<User, ApplicationError> handle(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            return Result.failure(emailAlreadyInUse());
        }
        var user = new User(
                command.name(),
                command.email(),
                hashingService.encode(command.password()),
                command.role()
        );
        try {
            return Result.success(userRepository.save(user));
        } catch (DataIntegrityViolationException ex) {
            return Result.failure(emailAlreadyInUse());
        }
    }

    @Override
    public Result<AuthenticatedUser, ApplicationError> handle(LoginCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty() || !hashingService.matches(command.password(), user.get().getPasswordHash())) {
            return Result.failure(ApplicationError.invalidCredentials());
        }
        var authenticatedUser = new AuthenticatedUser(
                user.get(),
                tokenService.generateAccessToken(user.get()),
                tokenService.generateRefreshToken(user.get()),
                tokenService.getAccessTokenExpirationSeconds()
        );
        return Result.success(authenticatedUser);
    }

    @Override
    @Transactional
    public Result<Boolean, ApplicationError> handle(ForgotPasswordCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Email", "No existe una cuenta asociada a ese correo"));
        }
        passwordResetTokenRepository.deleteByUserId(user.get().getId());
        var rawToken = ResetTokenCodec.generate();
        var expiresAt = Instant.now().plus(Duration.ofMinutes(resetExpirationMinutes));
        passwordResetTokenRepository.save(
                new PasswordResetToken(user.get().getId(), ResetTokenCodec.hash(rawToken), expiresAt));
        passwordResetMailSender.sendPasswordResetLink(
                user.get().getEmail(),
                user.get().getName(),
                resetPasswordUrl + "?token=" + rawToken,
                resetExpirationMinutes
        );
        return Result.success(true);
    }

    @Override
    @Transactional
    public Result<Boolean, ApplicationError> handle(ResetPasswordCommand command) {
        var now = Instant.now();
        var token = passwordResetTokenRepository.findByTokenHash(ResetTokenCodec.hash(command.token()));
        if (token.isEmpty() || !token.get().isUsable(now)) {
            return Result.failure(ApplicationError.validationError("El enlace de restablecimiento es inválido o ya expiró"));
        }
        var user = userRepository.findById(token.get().getUserId());
        if (user.isEmpty()) {
            return Result.failure(ApplicationError.validationError("El enlace de restablecimiento es inválido o ya expiró"));
        }
        user.get().changePasswordHash(hashingService.encode(command.newPassword()));
        token.get().markUsed(now);
        userRepository.save(user.get());
        passwordResetTokenRepository.save(token.get());
        return Result.success(true);
    }

    @Override
    @Transactional
    public Result<User, ApplicationError> handle(UpdateProfileCommand command) {
        var user = userRepository.findById(command.userId());
        if (user.isEmpty()) {
            return Result.failure(userNotFound());
        }
        user.get().updateProfile(command.name(), command.phone());
        return Result.success(userRepository.save(user.get()));
    }

    @Override
    @Transactional
    public Result<User, ApplicationError> handle(UpdateProfilePhotoCommand command) {
        var user = userRepository.findById(command.userId());
        if (user.isEmpty()) {
            return Result.failure(userNotFound());
        }
        var photo = profilePhotoRepository.findById(command.userId())
                .map(existing -> {
                    existing.replace(command.image());
                    return existing;
                })
                .orElseGet(() -> new ProfilePhoto(command.userId(), command.image()));
        profilePhotoRepository.save(photo);
        user.get().changePhoto(command.image().contentType());
        return Result.success(userRepository.save(user.get()));
    }

    private static ApplicationError emailAlreadyInUse() {
        return ApplicationError.conflict("Email", "El correo ya está en uso");
    }

    private static ApplicationError userNotFound() {
        return ApplicationError.notFound("User", "El usuario no existe");
    }
}
