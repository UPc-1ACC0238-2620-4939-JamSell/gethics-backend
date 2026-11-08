package com.jamsell.gethics.iam.domain.services;

import com.jamsell.gethics.iam.domain.model.aggregates.User;
import com.jamsell.gethics.iam.domain.model.commands.ForgotPasswordCommand;
import com.jamsell.gethics.iam.domain.model.commands.LoginCommand;
import com.jamsell.gethics.iam.domain.model.commands.RegisterUserCommand;
import com.jamsell.gethics.iam.domain.model.commands.ResetPasswordCommand;
import com.jamsell.gethics.iam.domain.model.commands.UpdateProfileCommand;
import com.jamsell.gethics.iam.domain.model.commands.UpdateProfilePhotoCommand;
import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.application.result.Result;

public interface UserCommandService {

    Result<User, ApplicationError> handle(RegisterUserCommand command);

    Result<AuthenticatedUser, ApplicationError> handle(LoginCommand command);

    Result<Boolean, ApplicationError> handle(ForgotPasswordCommand command);

    Result<Boolean, ApplicationError> handle(ResetPasswordCommand command);

    Result<User, ApplicationError> handle(UpdateProfileCommand command);

    Result<User, ApplicationError> handle(UpdateProfilePhotoCommand command);

    record AuthenticatedUser(User user, String accessToken, String refreshToken, long expiresInSeconds) {
    }
}
