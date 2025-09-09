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
 * Service focused purely on document comparison logic.
 * Does not handle searches - expects normalized documents as input.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiffService {
    
    private final ContentComparator contentComparator;
    private final ResultSetComparator resultSetComparator;
    private final HitCountComparator hitCountComparator;
    
    
    /**
     * Performs complete diff analysis with provided normalized documents.
     * Accepts raw SearchResults for hit count comparison and normalized documents for content comparison.
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
     * Compares two sets of normalized documents and returns the differences.
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
    
    @lombok.Data
    @lombok.Builder
    public static class DiffAnalysis {
        private ResultSetComparator.SetDifference setDifference;
        private ComparisonResult contentResult;
    }
}