package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import dev.aparikh.searchdifftool.normalization.FieldMappingConfigurationProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentComparator {
    
    private final FieldMappingConfigurationProperties fieldMappingConfigurationProperties;
    
    public ComparisonResult compareDocuments(List<NormalizedDocument> primaryDocs,
                                           List<NormalizedDocument> secondaryDocs) {
        
        // Create maps for quick lookup
        Map<String, NormalizedDocument> primaryMap = primaryDocs.stream()
            .collect(Collectors.toMap(NormalizedDocument::getId, doc -> doc));
            
        Map<String, NormalizedDocument> secondaryMap = secondaryDocs.stream()
            .collect(Collectors.toMap(NormalizedDocument::getId, doc -> doc));
        
        List<DiffResult.ContentDifference> differences = new ArrayList<>();
        Map<String, Integer> differencesByField = new HashMap<>();
        long documentsWithDifferences = 0;
        
        // Compare documents that exist in both sets
        for (Map.Entry<String, NormalizedDocument> entry : primaryMap.entrySet()) {
            String docId = entry.getKey();
            if (secondaryMap.containsKey(docId)) {
                NormalizedDocument primaryDoc = entry.getValue();
                NormalizedDocument secondaryDoc = secondaryMap.get(docId);
                
                List<DiffResult.ContentDifference> docDifferences = compareDocument(primaryDoc, secondaryDoc);
                differences.addAll(docDifferences);
                
                if (!docDifferences.isEmpty()) {
                    documentsWithDifferences++;
                    
                    // Count differences by field
                    for (DiffResult.ContentDifference diff : docDifferences) {
                        differencesByField.merge(diff.getField(), 1, Integer::sum);
                    }
                }
            }
        }
        
        log.info("Content comparison completed - {} field differences across {} documents", 
            differences.size(), documentsWithDifferences);
        
        return ComparisonResult.builder()
            .differences(differences)
            .documentsWithDifferences(documentsWithDifferences)
            .totalFieldDifferences((long) differences.size())
            .differencesByField(differencesByField)
            .build();
    }
    
    private List<DiffResult.ContentDifference> compareDocument(NormalizedDocument primary, NormalizedDocument secondary) {
        List<DiffResult.ContentDifference> differences = new ArrayList<>();
        
        // Compare fields dynamically based on configuration
        fieldMappingConfigurationProperties.getFields().keySet().forEach(commonFieldName -> {
            Object primaryValue = getFieldValue(primary, commonFieldName);
            Object secondaryValue = getFieldValue(secondary, commonFieldName);
            compareField(differences, primary.getId(), commonFieldName, primaryValue, secondaryValue);
        });
        
        // Compare metadata if present
        if (primary.getMetadata() != null && secondary.getMetadata() != null) {
            compareMetadata(differences, primary.getId(), primary.getMetadata(), secondary.getMetadata());
        } else if (primary.getMetadata() != null || secondary.getMetadata() != null) {
            differences.add(DiffResult.ContentDifference.builder()
                .documentId(primary.getId())
                .field("metadata")
                .primaryValue(primary.getMetadata())
                .secondaryValue(secondary.getMetadata())
                .differenceType("MISSING")
                .build());
        }
        
        return differences;
    }
    
    private Object getFieldValue(NormalizedDocument document, String commonFieldName) {
        try {
            // Use getter method based on field name
            String getterName = "get" + capitalize(commonFieldName);
            Method getter = NormalizedDocument.class.getMethod(getterName);
            return getter.invoke(document);
        } catch (Exception e) {
            log.warn("Failed to access field '{}' on NormalizedDocument: {}", commonFieldName, e.getMessage());
        }
        return null;
    }
    
    private String capitalize(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
    
    private void compareField(List<DiffResult.ContentDifference> differences, String docId, 
                             String fieldName, Object primaryValue, Object secondaryValue) {
        
        if (!Objects.equals(primaryValue, secondaryValue)) {
            String differenceType = determineDifferenceType(primaryValue, secondaryValue);
            
            differences.add(DiffResult.ContentDifference.builder()
                .documentId(docId)
                .field(fieldName)
                .primaryValue(primaryValue)
                .secondaryValue(secondaryValue)
                .differenceType(differenceType)
                .build());
        }
    }
    
    private void compareMetadata(List<DiffResult.ContentDifference> differences, String docId,
                                Map<String, Object> primaryMetadata, Map<String, Object> secondaryMetadata) {
        
        // Check all keys from primary
        for (Map.Entry<String, Object> entry : primaryMetadata.entrySet()) {
            String key = entry.getKey();
            Object primaryValue = entry.getValue();
            Object secondaryValue = secondaryMetadata.get(key);
            
            if (!Objects.equals(primaryValue, secondaryValue)) {
                compareField(differences, docId, "metadata." + key, primaryValue, secondaryValue);
            }
        }
        
        // Check for keys only in secondary
        for (Map.Entry<String, Object> entry : secondaryMetadata.entrySet()) {
            String key = entry.getKey();
            if (!primaryMetadata.containsKey(key)) {
                compareField(differences, docId, "metadata." + key, null, entry.getValue());
            }
        }
    }
    
    private String determineDifferenceType(Object primaryValue, Object secondaryValue) {
        if (primaryValue == null && secondaryValue != null) {
            return "MISSING_IN_PRIMARY";
        } else if (primaryValue != null && secondaryValue == null) {
            return "MISSING_IN_SECONDARY";
        } else if (primaryValue != null && secondaryValue != null && 
                   !primaryValue.getClass().equals(secondaryValue.getClass())) {
            return "TYPE_MISMATCH";
        } else {
            return "DIFFERENT";
        }
    }
    
    @Data
    @lombok.Builder
    public static class ComparisonResult {
        private List<DiffResult.ContentDifference> differences;
        private long documentsWithDifferences;
        private long totalFieldDifferences;
        private Map<String, Integer> differencesByField;
        
        public ComparisonResult() {
            this.differences = new ArrayList<>();
            this.documentsWithDifferences = 0;
            this.totalFieldDifferences = 0;
            this.differencesByField = new HashMap<>();
        }
        
        public ComparisonResult(List<DiffResult.ContentDifference> differences,
                               long documentsWithDifferences,
                               long totalFieldDifferences,
                               Map<String, Integer> differencesByField) {
            this.differences = differences != null ? differences : new ArrayList<>();
            this.documentsWithDifferences = documentsWithDifferences;
            this.totalFieldDifferences = totalFieldDifferences;
            this.differencesByField = differencesByField != null ? differencesByField : new HashMap<>();
        }
    }
}