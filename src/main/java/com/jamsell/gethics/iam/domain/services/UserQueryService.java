package com.jamsell.gethics.iam.domain.services;

import com.jamsell.gethics.iam.domain.model.aggregates.User;
import com.jamsell.gethics.iam.domain.model.entities.ProfilePhoto;
import com.jamsell.gethics.iam.domain.model.queries.GetProfilePhotoByUserIdQuery;
import com.jamsell.gethics.iam.domain.model.queries.GetUserByIdQuery;

import java.util.Optional;

public interface UserQueryService {

    Optional<User> handle(GetUserByIdQuery query);

    Optional<ProfilePhoto> handle(GetProfilePhotoByUserIdQuery query);
}
