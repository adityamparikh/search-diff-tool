package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Component responsible for identifying documents that exist in only one of the two result sets.
 * 
 * <p>This comparator performs set-based analysis to determine structural differences between
 * primary and secondary search results by comparing document IDs. It identifies documents
 * that are unique to either the primary or secondary system, which helps understand whether
 * the systems are returning different sets of documents for the same search criteria.
 * 
 * <p>Key features:
 * <ul>
 * <li><strong>Set-based Comparison:</strong> Uses document IDs to identify set differences</li>
 * <li><strong>Null Safety:</strong> Handles null document lists gracefully</li>
 * <li><strong>Performance Optimized:</strong> Uses Set operations for efficient comparison</li>
 * <li><strong>Detailed Logging:</strong> Provides comprehensive logging of differences found</li>
 * </ul>
 * 
 * <p>The comparison result identifies documents that exist only in the primary system,
 * only in the secondary system, or in both systems. This analysis is typically performed
 * before content comparison to understand the structural differences between result sets.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 * @see NormalizedDocument
 * @see SetDifference
 */
@Slf4j
@Component
public class ResultSetComparator {
    
    /**
     * Identifies documents that exist in only one of the two document sets.
     * 
     * <p>This method performs a set-based comparison using document IDs to identify:
     * <ul>
     * <li>Documents that exist only in the primary set</li>
     * <li>Documents that exist only in the secondary set</li>
     * </ul>
     * 
     * <p>The method safely handles null input lists by treating them as empty sets.
     * All differences are logged for monitoring and debugging purposes.
     * 
     * @param primaryDocs list of documents from primary system (null-safe)
     * @param secondaryDocs list of documents from secondary system (null-safe)
     * @return set difference analysis containing IDs of documents unique to each set
     */
    public SetDifference findDifferences(List<NormalizedDocument> primaryDocs,
                                       List<NormalizedDocument> secondaryDocs) {
        
        Set<String> primaryIds = primaryDocs != null ? primaryDocs.stream()
            .map(NormalizedDocument::getId)
            .collect(Collectors.toSet()) : Set.of();
            
        Set<String> secondaryIds = secondaryDocs != null ? secondaryDocs.stream()
            .map(NormalizedDocument::getId)
            .collect(Collectors.toSet()) : Set.of();
        
        List<String> onlyInPrimary = primaryIds.stream()
            .filter(id -> !secondaryIds.contains(id))
            .toList();

        List<String> onlyInSecondary = secondaryIds.stream()
            .filter(id -> !primaryIds.contains(id))
            .toList();
        
        log.info("Set differences - Only in primary: {}, Only in secondary: {}", 
            onlyInPrimary.size(), onlyInSecondary.size());
        
        if (!onlyInPrimary.isEmpty()) {
            log.debug("Documents only in primary: {}", onlyInPrimary);
        }
        
        if (!onlyInSecondary.isEmpty()) {
            log.debug("Documents only in secondary: {}", onlyInSecondary);
        }
        
        return SetDifference.builder()
            .onlyInPrimary(onlyInPrimary)
            .onlyInSecondary(onlyInSecondary)
            .build();
    }
    
    /**
     * Data class containing the results of set difference analysis.
     * 
     * <p>This class holds the document IDs that are unique to either the primary
     * or secondary result sets, enabling analysis of structural differences between
     * the two search systems.
     * 
     * <p>Empty lists indicate no unique documents in the respective system.
     */
    @Data
    @lombok.Builder
    public static class SetDifference {
        /** List of document IDs found only in the primary result set */
        private List<String> onlyInPrimary;
        /** List of document IDs found only in the secondary result set */
        private List<String> onlyInSecondary;
    }
}