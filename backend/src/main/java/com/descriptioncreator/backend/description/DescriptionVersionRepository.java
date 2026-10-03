package com.descriptioncreator.backend.description;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DescriptionVersionRepository
        extends JpaRepository<DescriptionVersionEntity, Long> {

    List<DescriptionVersionEntity> findAllByHandleOrderByCreatedAtDescIdDesc(String handle);

    Optional<DescriptionVersionEntity> findByIdAndHandle(Long id, String handle);
}
