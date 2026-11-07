package com.jamsell.gethics.iam.application.internal.queryservices;

import com.jamsell.gethics.iam.domain.model.aggregates.User;
import com.jamsell.gethics.iam.domain.model.entities.ProfilePhoto;
import com.jamsell.gethics.iam.domain.model.queries.GetProfilePhotoByUserIdQuery;
import com.jamsell.gethics.iam.domain.model.queries.GetUserByIdQuery;
import com.jamsell.gethics.iam.domain.services.UserQueryService;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.ProfilePhotoRepository;
import com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final ProfilePhotoRepository profilePhotoRepository;

    public UserQueryServiceImpl(UserRepository userRepository, ProfilePhotoRepository profilePhotoRepository) {
        this.userRepository = userRepository;
        this.profilePhotoRepository = profilePhotoRepository;
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public Optional<ProfilePhoto> handle(GetProfilePhotoByUserIdQuery query) {
        return profilePhotoRepository.findById(query.userId());
    }
}
