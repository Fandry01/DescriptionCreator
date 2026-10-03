package com.descriptioncreator.backend.description;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:description_versions;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class DescriptionVersionRepositoryTests {

    @Autowired
    private DescriptionVersionRepository repository;

    @Test
    void returnsOnlyRequestedHandleNewestFirst() {
        DescriptionVersionEntity oldest = save("bag", "first", Instant.parse("2026-01-01T00:00:00Z"));
        DescriptionVersionEntity newest = save("bag", "second", Instant.parse("2026-01-02T00:00:00Z"));
        save("another-bag", "unrelated", Instant.parse("2026-01-03T00:00:00Z"));

        assertThat(repository.findAllByHandleOrderByCreatedAtDescIdDesc("bag"))
                .extracting(DescriptionVersionEntity::getId)
                .containsExactly(newest.getId(), oldest.getId());
    }

    private DescriptionVersionEntity save(String handle, String publishedHtml, Instant createdAt) {
        return repository.saveAndFlush(new DescriptionVersionEntity(
                "gid://shopify/Product/1",
                handle,
                null,
                publishedHtml,
                DescriptionVersionAction.PUBLISH,
                null,
                createdAt
        ));
    }
}
