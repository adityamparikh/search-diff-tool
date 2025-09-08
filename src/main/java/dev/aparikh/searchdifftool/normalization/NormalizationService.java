package dev.aparikh.searchdifftool.normalization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class NormalizationService {
    
    private final FieldMappingConfigurationProperties fieldMappingConfigurationProperties;
    
    public NormalizedDocument normalize(SolrDocument doc, boolean isPrimary) {
        Map<String, Object> commonDoc = mapDocumentToCommon(doc, isPrimary);
        
        // Dynamically build NormalizedDocument using configuration and reflection with setters
        Map<String, FieldMappingConfigurationProperties.FieldMapping> fields = fieldMappingConfigurationProperties.getFields();
        
        try {
            // Create new instance using no-args constructor
            NormalizedDocument document = new NormalizedDocument();
            
            // Iterate through all configured fields dynamically
            for (Map.Entry<String, FieldMappingConfigurationProperties.FieldMapping> entry : fields.entrySet()) {
                String fieldName = entry.getKey();
                String fieldType = entry.getValue().type();
                Object value = getValueByType(commonDoc, fieldName, fieldType);

                // Use reflection to call setter method based on field name
                String setterName = "set" + capitalize(fieldName);
                try {
                    Method setter = NormalizedDocument.class.getMethod(setterName, getFieldType(fieldType));
                    setter.invoke(document, value);
                } catch (NoSuchMethodException e) {
                    // Skip fields that don't have corresponding setters - they'll be available in metadata
                    log.debug("No setter found for field '{}', skipping", fieldName);
                }
            }
            
            // Set metadata and source using setters
            document.setMetadata(commonDoc);
            document.setSource(isPrimary ? "primary" : "secondary");
            
            return document;
            
        } catch (Exception e) {
            log.error("Failed to create NormalizedDocument using reflection", e);
            throw new IllegalStateException("Failed to create NormalizedDocument using reflection", e);
        }
    }
    
    private Object getValueByType(Map<String, Object> doc, String fieldName, String dataType) {
        return switch (dataType.toLowerCase()) {
            case "string" -> getStringValue(doc, fieldName);
            case "date" -> getDateValue(doc, fieldName);
            case "list" -> getListValue(doc, fieldName);
            default -> {
                log.warn("Unknown data type '{}' for field '{}', treating as string", dataType, fieldName);
                yield getStringValue(doc, fieldName);
            }
        };
    }
    
    private String getStringValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        return value != null ? value.toString() : null;
    }
    
    private LocalDateTime getDateValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getListValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }

    public Map<String, Object> mapDocumentToCommon(SolrDocument doc, boolean isPrimary) {
        Map<String, String> mapping = isPrimary ?
                fieldMappingConfigurationProperties.getPrimaryToCommon() :
                fieldMappingConfigurationProperties.getSecondaryToCommon();

        Map<String, Object> commonDoc = new HashMap<>();

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String solrField = entry.getKey();
            String commonField = entry.getValue();

            if (doc.containsKey(solrField)) {
                commonDoc.put(commonField, doc.getFieldValue(solrField));
            }
        }

        // Also include any unmapped fields
        for (String fieldName : doc.getFieldNames()) {
            if (!mapping.containsKey(fieldName)) {
                commonDoc.put(fieldName, doc.getFieldValue(fieldName));
            }
        }

        return commonDoc;
    }
    
    private String capitalize(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
    
    private Class<?> getFieldType(String dataType) {
        return switch (dataType.toLowerCase()) {
            case "string" -> String.class;
            case "date" -> LocalDateTime.class;
            case "list" -> List.class;
            default -> String.class; // Default to string for unknown types
        };
    }
}
