package dev.aparikh.searchdifftool.solr.schema;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "schema.mapping")
public class FieldMappingConfig {
    
    private Map<String, String> primaryToCommon = new HashMap<>();
    private Map<String, String> secondaryToCommon = new HashMap<>();
    
    public FieldMappingConfig() {
        // Default mappings - can be overridden in application.properties
        initializeDefaultMappings();
    }
    
    private void initializeDefaultMappings() {
        // Primary system field mappings
        primaryToCommon.put("email_subject", "subject");
        primaryToCommon.put("email_from", "from");
        primaryToCommon.put("email_to", "to");
        primaryToCommon.put("email_body", "body");
        primaryToCommon.put("email_date", "date");
        primaryToCommon.put("email_id", "id");
        primaryToCommon.put("email_attachments", "attachments");
        
        // Secondary system field mappings
        secondaryToCommon.put("subject_field", "subject");
        secondaryToCommon.put("from_field", "from");
        secondaryToCommon.put("to_field", "to");
        secondaryToCommon.put("body_field", "body");
        secondaryToCommon.put("date_field", "date");
        secondaryToCommon.put("message_id", "id");
        secondaryToCommon.put("attachment_list", "attachments");
    }
}