package dev.aparikh.searchdifftool.solr.schema;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "schema.mapping")
public class FieldMappingConfig {
    
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
        primaryToCommon.clear();
        secondaryToCommon.clear();
        
        for (Map.Entry<String, FieldMapping> entry : fields.entrySet()) {
            String commonField = entry.getKey();
            FieldMapping mapping = entry.getValue();
            
            if (mapping.getPrimary() != null) {
                primaryToCommon.put(mapping.getPrimary(), commonField);
            }
            if (mapping.getSecondary() != null) {
                secondaryToCommon.put(mapping.getSecondary(), commonField);
            }
        }
    }
    
    @Data
    public static class FieldMapping {
        private String primary;
        private String secondary;
        
        public FieldMapping() {}
        
        public FieldMapping(String primary, String secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FieldMappingConfig.class);
}