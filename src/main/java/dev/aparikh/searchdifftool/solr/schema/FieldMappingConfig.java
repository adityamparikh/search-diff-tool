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
    
    private Map<String, String> primaryToCommon = new HashMap<>();
    private Map<String, String> secondaryToCommon = new HashMap<>();
    
    @PostConstruct
    public void validateConfiguration() {
        if (primaryToCommon.isEmpty()) {
            initializeDefaultMappings();
            log.warn("No primary field mappings configured, using default mappings");
        }
        if (secondaryToCommon.isEmpty()) {
            if (primaryToCommon.isEmpty()) {
                initializeDefaultMappings(); // This will set both if both are empty
            } else {
                initializeDefaultSecondaryMappings();
            }
            log.warn("No secondary field mappings configured, using default mappings");
        }
        
        log.info("Loaded {} primary field mappings and {} secondary field mappings",
                primaryToCommon.size(), secondaryToCommon.size());
    }
    
    private void initializeDefaultMappings() {
        initializeDefaultPrimaryMappings();
        initializeDefaultSecondaryMappings();
    }
    
    private void initializeDefaultPrimaryMappings() {
        primaryToCommon.put("email_subject", "subject");
        primaryToCommon.put("email_from", "from");
        primaryToCommon.put("email_to", "to");
        primaryToCommon.put("email_body", "body");
        primaryToCommon.put("email_date", "date");
        primaryToCommon.put("email_id", "id");
        primaryToCommon.put("email_attachments", "attachments");
    }
    
    private void initializeDefaultSecondaryMappings() {
        secondaryToCommon.put("subject_field", "subject");
        secondaryToCommon.put("from_field", "from");
        secondaryToCommon.put("to_field", "to");
        secondaryToCommon.put("body_field", "body");
        secondaryToCommon.put("date_field", "date");
        secondaryToCommon.put("message_id", "id");
        secondaryToCommon.put("attachment_list", "attachments");
    }
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FieldMappingConfig.class);
}