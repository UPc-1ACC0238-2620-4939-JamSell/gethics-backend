package com.jamsell.gethics.iam.interfaces.rest.transform;

import com.jamsell.gethics.iam.domain.model.commands.LoginCommand;
import com.jamsell.gethics.iam.interfaces.rest.resources.LoginResource;

public class LoginCommandFromResourceAssembler {

    public static LoginCommand toCommandFromResource(LoginResource resource) {
        return new LoginCommand(resource.email(), resource.password());
    }
}
