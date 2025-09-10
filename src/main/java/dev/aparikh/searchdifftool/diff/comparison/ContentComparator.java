package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.DiffResult;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import dev.aparikh.searchdifftool.normalization.FieldMappingConfigurationProperties;
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

/**
 * Component responsible for performing detailed field-level content comparison between documents.
 * 
 * <p>This comparator analyzes documents that exist in both primary and secondary result sets,
 * identifying differences at the individual field level. It uses reflection to dynamically
 * compare all configured fields and provides detailed reporting of differences found.
 * 
 * <p>The comparison process includes:
 * <ul>
 * <li><strong>Field-by-Field Analysis:</strong> Compares each configured field using reflection</li>
 * <li><strong>Null Value Handling:</strong> Properly handles missing fields in either dataset</li>
 * <li><strong>Type Mismatch Detection:</strong> Identifies fields with different data types</li>
 * <li><strong>Metadata Comparison:</strong> Deep comparison of nested metadata objects</li>
 * <li><strong>Difference Classification:</strong> Categorizes differences by type for analysis</li>
 * </ul>
 * 
 * <p>Results are aggregated into comprehensive metrics including total differences,
 * affected document counts, and per-field difference statistics.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 * @see DifferenceType
 * @see ComparisonResult
 * @see FieldMappingConfigurationProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentComparator {
    
    private final FieldMappingConfigurationProperties fieldMappingConfigurationProperties;
    
    /**
     * Performs comprehensive content comparison between two sets of normalized documents.
     * 
     * <p>This method analyzes documents with matching IDs from both sets, performing
     * field-by-field comparison to identify content differences. The analysis includes:
     * 
     * <ul>
     * <li>Standard field comparison (subject, body, from, etc.)</li>
     * <li>Deep metadata comparison for nested objects</li>
     * <li>Proper null value handling</li>
     * <li>Type mismatch detection</li>
     * </ul>
     * 
     * <p>Only documents that exist in both sets are compared for content differences.
     * Documents that exist in only one set should be identified using {@link ResultSetComparator}.
     * 
     * @param primaryDocs normalized documents from primary system
     * @param secondaryDocs normalized documents from secondary system
     * @return comprehensive comparison result with detailed difference analysis
     * @see DifferenceType for types of differences that can be detected
     */
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
        
        return new ComparisonResult(
            differences,
            documentsWithDifferences,
            differences.size(),
            differencesByField
        );
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
                .differenceType(primary.getMetadata() != null ? DifferenceType.MISSING_IN_SECONDARY : DifferenceType.MISSING_IN_PRIMARY)
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
            DifferenceType differenceType = determineDifferenceType(primaryValue, secondaryValue);
            
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
    
    private DifferenceType determineDifferenceType(Object primaryValue, Object secondaryValue) {
        if (primaryValue == null && secondaryValue != null) {
            return DifferenceType.MISSING_IN_PRIMARY;
        } else if (primaryValue != null && secondaryValue == null) {
            return DifferenceType.MISSING_IN_SECONDARY;
        } else if (primaryValue != null && !primaryValue.getClass().equals(secondaryValue.getClass())) {
            return DifferenceType.TYPE_MISMATCH;
        } else {
            return DifferenceType.DIFFERENT;
        }
    }
    
}