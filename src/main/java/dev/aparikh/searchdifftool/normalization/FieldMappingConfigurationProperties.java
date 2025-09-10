package dev.aparikh.searchdifftool.normalization;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for field mapping between primary and secondary Solr systems.
 * 
 * <p>This configuration class manages the mapping of field names between different Solr schemas,
 * enabling the application to work with systems that may use different field naming conventions.
 * The configuration is loaded from application properties with the prefix "schema.mapping".
 * 
 * <p>Configuration structure in YAML:
 * <pre>
 * schema:
 *   mapping:
 *     fields:
 *       subject:
 *         primary: "email_subject"
 *         secondary: "subject_field"
 *         type: "string"
 *       body:
 *         primary: "email_body"
 *         secondary: "content"
 *         type: "string"
 * </pre>
 * 
 * <p>Key features:
 * <ul>
 * <li><strong>Flexible Mapping:</strong> Supports different field names for primary and secondary</li>
 * <li><strong>Type Information:</strong> Stores field type metadata for proper handling</li>
 * <li><strong>Reverse Lookup:</strong> Builds efficient lookup maps for both directions</li>
 * <li><strong>Validation:</strong> Ensures required mappings are configured at startup</li>
 * </ul>
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 * @see FieldMapping
 * @see NormalizationService
 */
@Data
@Component
@ConfigurationProperties(prefix = "schema.mapping")
public class FieldMappingConfigurationProperties {

    private static final Logger log = LoggerFactory.getLogger(FieldMappingConfigurationProperties.class);

    /** Configuration structure: common field name -> field mapping details */
    private Map<String, FieldMapping> fields = new HashMap<>();

    /** Reverse lookup map: primary field name -> common field name */
    private Map<String, String> primaryToCommon = new HashMap<>();
    /** Reverse lookup map: secondary field name -> common field name */
    private Map<String, String> secondaryToCommon = new HashMap<>();

    /**
     * Processes the field mapping configuration after properties are loaded.
     * 
     * <p>This method is called automatically after Spring loads the configuration properties.
     * It validates that mappings are configured and builds reverse lookup maps for efficient
     * field name translation during document normalization.
     * 
     * @throws IllegalStateException if no field mappings are configured
     */
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

    /**
     * Builds reverse lookup maps for efficient field name translation.
     * 
     * <p>Creates mappings from primary/secondary field names back to common field names,
     * enabling efficient lookup during document normalization process.
     */
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

    /**
     * Record representing the mapping configuration for a single field.
     * 
     * <p>Contains the field names used in primary and secondary systems,
     * along with type information for proper data handling.
     * 
     * @param primary field name in the primary Solr system
     * @param secondary field name in the secondary Solr system  
     * @param type data type of the field (string, date, list, etc.)
     */
    public record FieldMapping(
        String primary,
        String secondary,
        String type) {}
}
