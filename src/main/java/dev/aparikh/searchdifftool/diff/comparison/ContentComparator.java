package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.email.model.EmailDocument;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ContentComparator {
    
    public ComparisonResult compareDocuments(List<EmailDocument> primaryDocs, 
                                           List<EmailDocument> secondaryDocs) {
        
        // Create maps for quick lookup
        Map<String, EmailDocument> primaryMap = primaryDocs.stream()
            .collect(Collectors.toMap(EmailDocument::getId, doc -> doc));
            
        Map<String, EmailDocument> secondaryMap = secondaryDocs.stream()
            .collect(Collectors.toMap(EmailDocument::getId, doc -> doc));
        
        List<DiffResult.ContentDifference> differences = new ArrayList<>();
        Map<String, Integer> differencesByField = new HashMap<>();
        long documentsWithDifferences = 0;
        
        // Compare documents that exist in both sets
        for (String docId : primaryMap.keySet()) {
            if (secondaryMap.containsKey(docId)) {
                EmailDocument primaryDoc = primaryMap.get(docId);
                EmailDocument secondaryDoc = secondaryMap.get(docId);
                
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
    
    private List<DiffResult.ContentDifference> compareDocument(EmailDocument primary, EmailDocument secondary) {
        List<DiffResult.ContentDifference> differences = new ArrayList<>();
        
        // Compare each field
        compareField(differences, primary.getId(), "subject", primary.getSubject(), secondary.getSubject());
        compareField(differences, primary.getId(), "from", primary.getFrom(), secondary.getFrom());
        compareField(differences, primary.getId(), "to", primary.getTo(), secondary.getTo());
        compareField(differences, primary.getId(), "body", primary.getBody(), secondary.getBody());
        compareField(differences, primary.getId(), "date", primary.getDate(), secondary.getDate());
        compareField(differences, primary.getId(), "attachments", primary.getAttachments(), secondary.getAttachments());
        
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
        for (String key : secondaryMetadata.keySet()) {
            if (!primaryMetadata.containsKey(key)) {
                compareField(differences, docId, "metadata." + key, null, secondaryMetadata.get(key));
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