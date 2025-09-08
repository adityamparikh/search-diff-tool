package dev.aparikh.searchdifftool.solr.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "schema.mapping.primary-to-common.custom_field=normalized_custom",
    "schema.mapping.secondary-to-common.different_field=normalized_custom"
})
class FieldMappingConfigTest {

    @Autowired
    private FieldMappingConfig fieldMappingConfig;

    @Test
    void shouldLoadFieldMappingsFromConfiguration() {
        // Verify that custom configuration is loaded
        assertThat(fieldMappingConfig.getPrimaryToCommon())
                .containsEntry("custom_field", "normalized_custom");
        
        assertThat(fieldMappingConfig.getSecondaryToCommon())
                .containsEntry("different_field", "normalized_custom");
    }

    @Test
    void shouldLoadDefaultMappingsFromYaml() {
        // Verify that default mappings from YAML are loaded
        assertThat(fieldMappingConfig.getPrimaryToCommon())
                .containsEntry("email_subject", "subject")
                .containsEntry("email_from", "from")
                .containsEntry("email_to", "to");

        assertThat(fieldMappingConfig.getSecondaryToCommon())
                .containsEntry("subject_field", "subject")
                .containsEntry("from_field", "from")
                .containsEntry("to_field", "to");
    }

    @Test
    void shouldHaveNonEmptyMappings() {
        assertThat(fieldMappingConfig.getPrimaryToCommon()).isNotEmpty();
        assertThat(fieldMappingConfig.getSecondaryToCommon()).isNotEmpty();
        
        // Should have at least the 7 default mappings plus the test custom mapping
        assertThat(fieldMappingConfig.getPrimaryToCommon()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(fieldMappingConfig.getSecondaryToCommon()).hasSizeGreaterThanOrEqualTo(7);
    }
}