package com.jamsell.gethics.iam.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.iam.domain.model.entities.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, Long> {
}
