package dev.aparikh.searchdifftool.solr.schema;

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
    private FieldMappingConfig fieldMappingConfig;

    @Test
    void shouldLoadFieldMappingsFromConfiguration() {
        // Verify that custom configuration is loaded in the new structure
        assertThat(fieldMappingConfig.getFields())
                .containsKey("custom_field");
        
        FieldMappingConfig.FieldMapping customMapping = fieldMappingConfig.getFields().get("custom_field");
        assertThat(customMapping.primary()).isEqualTo("custom_primary_field");
        assertThat(customMapping.secondary()).isEqualTo("custom_secondary_field");
        assertThat(customMapping.type()).isEqualTo("string");
        
        // Verify backward compatibility maps are built correctly
        assertThat(fieldMappingConfig.getPrimaryToCommon())
                .containsEntry("custom_primary_field", "custom_field");
        
        assertThat(fieldMappingConfig.getSecondaryToCommon())
                .containsEntry("custom_secondary_field", "custom_field");
    }

    @Test
    void shouldLoadMappingsFromYaml() {
        // Verify that mappings from YAML are loaded in new structure
        assertThat(fieldMappingConfig.getFields())
                .containsKey("subject")
                .containsKey("from")
                .containsKey("to");

        // Verify the new structure
        FieldMappingConfig.FieldMapping subjectMapping = fieldMappingConfig.getFields().get("subject");
        assertThat(subjectMapping.primary()).isEqualTo("email_subject");
        assertThat(subjectMapping.secondary()).isEqualTo("subject_field");
        assertThat(subjectMapping.type()).isEqualTo("string");

        // Verify different data types
        FieldMappingConfig.FieldMapping dateMapping = fieldMappingConfig.getFields().get("date");
        assertThat(dateMapping.type()).isEqualTo("date");
        
        FieldMappingConfig.FieldMapping attachmentsMapping = fieldMappingConfig.getFields().get("attachments");
        assertThat(attachmentsMapping.type()).isEqualTo("list");

        // Verify backward compatibility maps are built correctly
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
        assertThat(fieldMappingConfig.getFields()).isNotEmpty();
        assertThat(fieldMappingConfig.getPrimaryToCommon()).isNotEmpty();
        assertThat(fieldMappingConfig.getSecondaryToCommon()).isNotEmpty();
        
        // Should have at least the 7 default mappings plus the test custom mapping
        assertThat(fieldMappingConfig.getFields()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(fieldMappingConfig.getPrimaryToCommon()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(fieldMappingConfig.getSecondaryToCommon()).hasSizeGreaterThanOrEqualTo(7);
    }

    @Test
    void shouldMaintainConsistencyBetweenNewAndOldStructures() {
        // Verify that the new structure and backward compatibility maps are consistent
        for (var entry : fieldMappingConfig.getFields().entrySet()) {
            String commonField = entry.getKey();
            FieldMappingConfig.FieldMapping mapping = entry.getValue();
            
            if (mapping.primary() != null) {
                assertThat(fieldMappingConfig.getPrimaryToCommon())
                        .containsEntry(mapping.primary(), commonField);
            }
            
            if (mapping.secondary() != null) {
                assertThat(fieldMappingConfig.getSecondaryToCommon())
                        .containsEntry(mapping.secondary(), commonField);
            }
        }
    }
}