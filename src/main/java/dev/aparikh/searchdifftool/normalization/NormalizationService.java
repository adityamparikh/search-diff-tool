package dev.aparikh.searchdifftool.normalization;

import dev.aparikh.searchdifftool.search.solr.schema.SchemaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NormalizationService {
    
    private final SchemaMapper schemaMapper;
    private final FieldMappingConfigurationProperties fieldMappingConfigurationProperties;
    
    public NormalizedDocument normalize(SolrDocument doc, boolean isPrimary) {
        Map<String, Object> commonDoc = schemaMapper.mapDocumentToCommon(doc, isPrimary);
        
        // Build field values array dynamically based on configuration
        Object[] fieldValues = new Object[9]; // NormalizedDocument has 9 fields
        
        // Get field values dynamically using configuration
        for (Map.Entry<String, FieldMappingConfigurationProperties.FieldMapping> entry : fieldMappingConfigurationProperties.getFields().entrySet()) {
            String commonFieldName = entry.getKey();
            String dataType = entry.getValue().type();
            
            Object value = getValueByType(commonDoc, commonFieldName, dataType);
            setFieldValueByName(fieldValues, commonFieldName, value);
        }
        
        // Set metadata and source
        fieldValues[7] = commonDoc; // metadata
        fieldValues[8] = isPrimary ? "primary" : "secondary"; // source
        
        return new NormalizedDocument(
            (String) fieldValues[0], // id
            (String) fieldValues[1], // subject  
            (String) fieldValues[2], // from
            (String) fieldValues[3], // to
            (String) fieldValues[4], // body
            (LocalDateTime) fieldValues[5], // date
            (List<String>) fieldValues[6], // attachments
            (Map<String, Object>) fieldValues[7], // metadata
            (String) fieldValues[8] // source
        );
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
    
    private void setFieldValueByName(Object[] fieldValues, String fieldName, Object value) {
        switch (fieldName) {
            case "id" -> fieldValues[0] = value;
            case "subject" -> fieldValues[1] = value;
            case "from" -> fieldValues[2] = value;
            case "to" -> fieldValues[3] = value;
            case "body" -> fieldValues[4] = value;
            case "date" -> fieldValues[5] = value;
            case "attachments" -> fieldValues[6] = value;
            default -> log.warn("Unknown field name: {}", fieldName);
        }
    }
    
    private String getStringValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        return value != null ? value.toString() : null;
    }
    
    private LocalDateTime getDateValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
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
}
