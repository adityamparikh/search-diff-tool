package dev.aparikh.searchdifftool.solr.schema;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "schema.mapping")
public class FieldMappingConfig {

    private static final Logger log = LoggerFactory.getLogger(FieldMappingConfig.class);

    // Configuration structure: common field name -> {primary: field, secondary: field}
    private Map<String, FieldMapping> fields = new HashMap<>();

    // Backward compatibility maps for existing SchemaMapper
    private Map<String, String> primaryToCommon = new HashMap<>();
    private Map<String, String> secondaryToCommon = new HashMap<>();

    @PostConstruct
    public void processConfiguration() {
        if (fields.isEmpty()) {
            throw new IllegalStateException("No field mappings configured in application.yaml. Please configure schema.mapping.fields.");
        }

        // Build the reverse lookup maps for SchemaMapper compatibility
        buildReverseMaps();

        log.info("Loaded {} field mappings with {} primary and {} secondary field mappings",
                fields.size(), primaryToCommon.size(), secondaryToCommon.size());
    }

    private void buildReverseMaps() {

        for (Map.Entry<String, FieldMapping> entry : fields.entrySet()) {
            String commonField = entry.getKey();
            FieldMapping mapping = entry.getValue();

            if (mapping.primary() != null) {
                primaryToCommon.put(mapping.primary(), commonField);
            }
            if (mapping.secondary() != null) {
                secondaryToCommon.put(mapping.secondary(), commonField);
            }
        }
    }

    public record FieldMapping(
        String primary,
        String secondary,
        String type) {}
}
