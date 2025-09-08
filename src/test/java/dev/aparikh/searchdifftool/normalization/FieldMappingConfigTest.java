package dev.aparikh.searchdifftool.normalization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "schema.mapping.fields.custom_field.primary=custom_primary_field",
    "schema.mapping.fields.custom_field.secondary=custom_secondary_field",
    "schema.mapping.fields.custom_field.type=string"
})
class FieldMappingConfigTest {

    @Autowired
    private FieldMappingConfigurationProperties fieldMappingConfigurationProperties;

    @Test
    void shouldLoadFieldMappingsFromConfiguration() {
        // Verify that custom configuration is loaded in the new structure
        assertThat(fieldMappingConfigurationProperties.getFields())
                .containsKey("custom_field");
        
        FieldMappingConfigurationProperties.FieldMapping customMapping = fieldMappingConfigurationProperties.getFields().get("custom_field");
        assertThat(customMapping.primary()).isEqualTo("custom_primary_field");
        assertThat(customMapping.secondary()).isEqualTo("custom_secondary_field");
        assertThat(customMapping.type()).isEqualTo("string");
        
        // Verify backward compatibility maps are built correctly
        assertThat(fieldMappingConfigurationProperties.getPrimaryToCommon())
                .containsEntry("custom_primary_field", "custom_field");
        
        assertThat(fieldMappingConfigurationProperties.getSecondaryToCommon())
                .containsEntry("custom_secondary_field", "custom_field");
    }

    @Test
    void shouldLoadMappingsFromYaml() {
        // Verify that mappings from YAML are loaded in new structure
        assertThat(fieldMappingConfigurationProperties.getFields())
                .containsKey("subject")
                .containsKey("from")
                .containsKey("to");

        // Verify the new structure
        FieldMappingConfigurationProperties.FieldMapping subjectMapping = fieldMappingConfigurationProperties.getFields().get("subject");
        assertThat(subjectMapping.primary()).isEqualTo("email_subject");
        assertThat(subjectMapping.secondary()).isEqualTo("subject_field");
        assertThat(subjectMapping.type()).isEqualTo("string");

        // Verify different data types
        FieldMappingConfigurationProperties.FieldMapping dateMapping = fieldMappingConfigurationProperties.getFields().get("date");
        assertThat(dateMapping.type()).isEqualTo("date");
        
        FieldMappingConfigurationProperties.FieldMapping attachmentsMapping = fieldMappingConfigurationProperties.getFields().get("attachments");
        assertThat(attachmentsMapping.type()).isEqualTo("list");

        // Verify backward compatibility maps are built correctly
        assertThat(fieldMappingConfigurationProperties.getPrimaryToCommon())
                .containsEntry("email_subject", "subject")
                .containsEntry("email_from", "from")
                .containsEntry("email_to", "to");

        assertThat(fieldMappingConfigurationProperties.getSecondaryToCommon())
                .containsEntry("subject_field", "subject")
                .containsEntry("from_field", "from")
                .containsEntry("to_field", "to");
    }

    @Test
    void shouldHaveNonEmptyMappings() {
        assertThat(fieldMappingConfigurationProperties.getFields()).isNotEmpty();
        assertThat(fieldMappingConfigurationProperties.getPrimaryToCommon()).isNotEmpty();
        assertThat(fieldMappingConfigurationProperties.getSecondaryToCommon()).isNotEmpty();
        
        // Should have at least the 7 default mappings plus the test custom mapping
        assertThat(fieldMappingConfigurationProperties.getFields()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(fieldMappingConfigurationProperties.getPrimaryToCommon()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(fieldMappingConfigurationProperties.getSecondaryToCommon()).hasSizeGreaterThanOrEqualTo(7);
    }

    @Test
    void shouldMaintainConsistencyBetweenNewAndOldStructures() {
        // Verify that the new structure and backward compatibility maps are consistent
        for (var entry : fieldMappingConfigurationProperties.getFields().entrySet()) {
            String commonField = entry.getKey();
            FieldMappingConfigurationProperties.FieldMapping mapping = entry.getValue();
            
            if (mapping.primary() != null) {
                assertThat(fieldMappingConfigurationProperties.getPrimaryToCommon())
                        .containsEntry(mapping.primary(), commonField);
            }
            
            if (mapping.secondary() != null) {
                assertThat(fieldMappingConfigurationProperties.getSecondaryToCommon())
                        .containsEntry(mapping.secondary(), commonField);
            }
        }
    }
}