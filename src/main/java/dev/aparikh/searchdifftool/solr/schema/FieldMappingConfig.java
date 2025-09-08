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
    
    // New structure: common field name -> {primary: field, secondary: field}
    private Map<String, FieldMapping> fields = new HashMap<>();
    
    // Maintain backward compatibility with existing SchemaMapper
    private Map<String, String> primaryToCommon = new HashMap<>();
    private Map<String, String> secondaryToCommon = new HashMap<>();
    
    @PostConstruct
    public void processConfiguration() {
        if (fields.isEmpty()) {
            initializeDefaultMappings();
            log.warn("No field mappings configured, using default mappings");
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
    
    private void initializeDefaultMappings() {
        fields.put("subject", new FieldMapping("email_subject", "subject_field"));
        fields.put("from", new FieldMapping("email_from", "from_field"));
        fields.put("to", new FieldMapping("email_to", "to_field"));
        fields.put("body", new FieldMapping("email_body", "body_field"));
        fields.put("date", new FieldMapping("email_date", "date_field"));
        fields.put("id", new FieldMapping("email_id", "message_id"));
        fields.put("attachments", new FieldMapping("email_attachments", "attachment_list"));
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