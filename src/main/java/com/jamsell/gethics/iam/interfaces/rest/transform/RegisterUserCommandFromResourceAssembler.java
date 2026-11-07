package com.jamsell.gethics.iam.interfaces.rest.transform;

import com.jamsell.gethics.iam.domain.model.commands.RegisterUserCommand;
import com.jamsell.gethics.iam.domain.model.valueobjects.Role;
import com.jamsell.gethics.iam.interfaces.rest.resources.RegisterUserResource;

public class RegisterUserCommandFromResourceAssembler {

    public static RegisterUserCommand toCommandFromResource(RegisterUserResource resource) {
        return new RegisterUserCommand(
                resource.name(),
                resource.email(),
                resource.password(),
                Role.fromName(resource.role())
        );
    }
}
