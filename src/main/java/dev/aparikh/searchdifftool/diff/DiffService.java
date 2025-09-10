package dev.aparikh.searchdifftool.diff;

import dev.aparikh.searchdifftool.diff.comparison.ContentComparator;
import dev.aparikh.searchdifftool.diff.comparison.ComparisonResult;
import dev.aparikh.searchdifftool.diff.comparison.HitCountComparator;
import dev.aparikh.searchdifftool.diff.comparison.ResultSetComparator;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import dev.aparikh.searchdifftool.search.SearchResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core service for performing comprehensive document difference analysis.
 * 
 * <p>This service is responsible for coordinating various comparison operations between
 * primary and secondary document sets. It focuses purely on comparison logic and does not
 * handle search operations - it expects pre-normalized documents as input.
 * 
 * <p>The service performs three types of comparisons:
 * <ul>
 * <li><strong>Hit Count Comparison:</strong> Compares total number of search results</li>
 * <li><strong>Result Set Comparison:</strong> Identifies documents present in only one set</li>
 * <li><strong>Content Comparison:</strong> Analyzes field-level differences in common documents</li>
 * </ul>
 * 
 * <p>All comparison operations are logged for monitoring and debugging purposes, and
 * performance metrics are collected to track operation efficiency.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 * @see ContentComparator
 * @see ResultSetComparator  
 * @see HitCountComparator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiffService {
    
    private final ContentComparator contentComparator;
    private final ResultSetComparator resultSetComparator;
    private final HitCountComparator hitCountComparator;
    
    
    /**
     * Performs complete comprehensive diff analysis between primary and secondary search results.
     * 
     * <p>This method coordinates all comparison operations and builds a complete diff result:
     * <ol>
     * <li>Compares hit counts using raw search results</li>
     * <li>Analyzes result set differences using normalized documents</li>
     * <li>Performs content comparison of common documents</li>
     * <li>Builds performance metrics and summary</li>
     * </ol>
     * 
     * @param primaryResult raw search result from primary system (used for hit count and timing)
     * @param secondaryResult raw search result from secondary system (used for hit count and timing)
     * @param primaryNormalizedDocs normalized documents from primary system
     * @param secondaryNormalizedDocs normalized documents from secondary system
     * @return comprehensive diff result containing all comparison analysis
     */
    public DiffResult performCompleteDiffWithNormalizedDocs(SearchResult primaryResult, 
                                                          SearchResult secondaryResult,
                                                          List<NormalizedDocument> primaryNormalizedDocs,
                                                          List<NormalizedDocument> secondaryNormalizedDocs) {
        
        log.debug("Performing complete diff analysis between {} primary and {} secondary hits", 
            primaryResult.totalHits(), secondaryResult.totalHits());
        
        long startTime = System.currentTimeMillis();
        DiffResult.DiffResultBuilder resultBuilder = DiffResult.builder();
        
        // Compare hit counts using raw search results
        DiffResult.HitCounts hitCounts = hitCountComparator.compare(primaryResult, secondaryResult);
        resultBuilder.hitCounts(hitCounts);
        resultBuilder.countsMatch(hitCounts.getDifference() == 0);
        
        // Perform document analysis using normalized documents
        DiffAnalysis analysis = compareDocs(primaryNormalizedDocs, secondaryNormalizedDocs);
        resultBuilder.onlyInPrimary(analysis.getSetDifference().getOnlyInPrimary());
        resultBuilder.onlyInSecondary(analysis.getSetDifference().getOnlyInSecondary());
        resultBuilder.contentDifferences(analysis.getContentResult().differences());
        
        // Build metrics
        long comparisonTime = System.currentTimeMillis() - startTime;
        DiffResult.ComparisonMetrics metrics = DiffResult.ComparisonMetrics.builder()
            .totalDocumentsCompared(Math.min(primaryNormalizedDocs.size(), secondaryNormalizedDocs.size()))
            .documentsWithDifferences(analysis.getContentResult().documentsWithDifferences())
            .totalFieldDifferences(analysis.getContentResult().totalFieldDifferences())
            .queryTimePrimary(primaryResult.queryTime())
            .queryTimeSecondary(secondaryResult.queryTime())
            .comparisonTimeMs(comparisonTime)
            .differencesByField(analysis.getContentResult().differencesByField())
            .build();
        resultBuilder.metrics(metrics);
        
        // Generate summary
        String summary = generateSummary(hitCounts, analysis.getSetDifference(), analysis.getContentResult());
        resultBuilder.summary(summary);
        
        return resultBuilder.build();
    }
    
    /**
     * Compares two sets of normalized documents for structural and content differences.
     * 
     * <p>This method performs two main comparison operations:
     * <ul>
     * <li><strong>Set Difference Analysis:</strong> Identifies documents present in only one set</li>
     * <li><strong>Content Comparison:</strong> Analyzes field-level differences in documents present in both sets</li>
     * </ul>
     * 
     * @param primaryDocs normalized documents from primary system
     * @param secondaryDocs normalized documents from secondary system
     * @return analysis containing both set differences and content comparison results
     */
    public DiffAnalysis compareDocs(List<NormalizedDocument> primaryDocs, 
                                   List<NormalizedDocument> secondaryDocs) {
        
        log.debug("Comparing {} primary documents with {} secondary documents", 
            primaryDocs.size(), secondaryDocs.size());
        
        // Find documents only in one result set
        ResultSetComparator.SetDifference setDifference = resultSetComparator.findDifferences(primaryDocs, secondaryDocs);
        
        // Compare content of common documents
        ComparisonResult contentDifferences = contentComparator.compareDocuments(primaryDocs, secondaryDocs);
        
        log.debug("Found {} documents only in primary, {} only in secondary, {} content differences",
            setDifference.getOnlyInPrimary().size(),
            setDifference.getOnlyInSecondary().size(),
            contentDifferences.totalFieldDifferences());
        
        return DiffAnalysis.builder()
            .setDifference(setDifference)
            .contentResult(contentDifferences)
            .build();
    }
    
    /**
     * Generates a human-readable summary of the diff analysis results.
     * 
     * <p>The summary includes:
     * <ul>
     * <li>Hit count comparison results</li>
     * <li>Documents found only in primary or secondary</li>
     * <li>Content difference statistics</li>
     * <li>Overall match status</li>
     * </ul>
     * 
     * @param hitCounts comparison of total hit counts
     * @param setDifference documents found only in one set
     * @param contentResult field-level content differences
     * @return formatted summary string
     */
    private String generateSummary(DiffResult.HitCounts hitCounts, 
                                 ResultSetComparator.SetDifference setDifference,
                                 ComparisonResult contentResult) {
        StringBuilder summary = new StringBuilder();
        
        if (hitCounts.getDifference() == 0) {
            summary.append("Hit counts match (").append(hitCounts.getPrimary()).append("). ");
        } else {
            summary.append("Hit counts differ by ").append(Math.abs(hitCounts.getDifference()))
                   .append(" (Primary: ").append(hitCounts.getPrimary())
                   .append(", Secondary: ").append(hitCounts.getSecondary()).append("). ");
        }
        
        if (!setDifference.getOnlyInPrimary().isEmpty()) {
            summary.append(setDifference.getOnlyInPrimary().size()).append(" documents only in primary. ");
        }
        
        if (!setDifference.getOnlyInSecondary().isEmpty()) {
            summary.append(setDifference.getOnlyInSecondary().size()).append(" documents only in secondary. ");
        }
        
        if (contentResult.totalFieldDifferences() > 0) {
            summary.append("Found ").append(contentResult.totalFieldDifferences())
                   .append(" field differences across ").append(contentResult.documentsWithDifferences())
                   .append(" documents. ");
        } else if (hitCounts.getDifference() == 0 && setDifference.getOnlyInPrimary().isEmpty() && setDifference.getOnlyInSecondary().isEmpty()) {
            summary.append("Data matches completely.");
        }
        
        return summary.toString().trim();
    }
    
    /**
     * Data class containing the results of document comparison analysis.
     * 
     * <p>This class aggregates results from both set difference analysis
     * and content comparison operations for internal processing.
     */
    @lombok.Data
    @lombok.Builder
    public static class DiffAnalysis {
        /** Documents found only in primary or secondary sets */
        private ResultSetComparator.SetDifference setDifference;
        /** Field-level content differences in common documents */
        private ComparisonResult contentResult;
    }
}