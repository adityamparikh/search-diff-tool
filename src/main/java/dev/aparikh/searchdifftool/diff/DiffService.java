package dev.aparikh.searchdifftool.diff;

import dev.aparikh.searchdifftool.diff.comparison.HitCountComparator;
import dev.aparikh.searchdifftool.diff.comparison.ContentComparator;
import dev.aparikh.searchdifftool.diff.comparison.ResultSetComparator;
import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.email.EmailSearchService;
import dev.aparikh.searchdifftool.email.model.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiffService {
    
    private final EmailSearchService emailSearchService;
    private final HitCountComparator hitCountComparator;
    private final ContentComparator contentComparator;
    private final ResultSetComparator resultSetComparator;
    
    public DiffResult performDiff(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting diff comparison for criteria: {}", criteria);
        
        // Execute searches on both systems
        EmailSearchService.SearchResult primaryResult = emailSearchService.searchPrimary(criteria);
        EmailSearchService.SearchResult secondaryResult = emailSearchService.searchSecondary(criteria);
        
        log.info("Primary search returned {} hits, Secondary search returned {} hits", 
            primaryResult.getTotalHits(), secondaryResult.getTotalHits());
        
        // Build diff result
        DiffResult.DiffResultBuilder resultBuilder = DiffResult.builder();
        
        // Compare hit counts
        DiffResult.HitCounts hitCounts = hitCountComparator.compare(primaryResult, secondaryResult);
        resultBuilder.hitCounts(hitCounts);
        resultBuilder.countsMatch(hitCounts.getDifference() == 0);
        
        // Find documents only in one result set
        ResultSetComparator.SetDifference setDifference = resultSetComparator.findDifferences(
            primaryResult.getDocuments(), secondaryResult.getDocuments());
        resultBuilder.onlyInPrimary(setDifference.getOnlyInPrimary());
        resultBuilder.onlyInSecondary(setDifference.getOnlyInSecondary());
        
        // Compare content of common documents
        var contentDifferences = contentComparator.compareDocuments(
            primaryResult.getDocuments(), secondaryResult.getDocuments());
        resultBuilder.contentDifferences(contentDifferences.getDifferences());
        
        // Build metrics
        long comparisonTime = System.currentTimeMillis() - startTime;
        DiffResult.ComparisonMetrics metrics = DiffResult.ComparisonMetrics.builder()
            .totalDocumentsCompared(Math.min(primaryResult.getDocuments().size(), secondaryResult.getDocuments().size()))
            .documentsWithDifferences(contentDifferences.getDocumentsWithDifferences())
            .totalFieldDifferences(contentDifferences.getTotalFieldDifferences())
            .queryTimePrimary(primaryResult.getQueryTime())
            .queryTimeSecondary(secondaryResult.getQueryTime())
            .comparisonTimeMs(comparisonTime)
            .differencesByField(contentDifferences.getDifferencesByField())
            .build();
        resultBuilder.metrics(metrics);
        
        // Generate summary
        String summary = generateSummary(hitCounts, setDifference, contentDifferences);
        resultBuilder.summary(summary);
        
        DiffResult result = resultBuilder.build();
        log.info("Diff comparison completed: {}", summary);
        
        return result;
    }
    
    private String generateSummary(DiffResult.HitCounts hitCounts, 
                                 ResultSetComparator.SetDifference setDifference,
                                 ContentComparator.ComparisonResult contentResult) {
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
        
        if (contentResult.getTotalFieldDifferences() > 0) {
            summary.append("Found ").append(contentResult.getTotalFieldDifferences())
                   .append(" field differences across ").append(contentResult.getDocumentsWithDifferences())
                   .append(" documents. ");
        } else if (hitCounts.getDifference() == 0 && setDifference.getOnlyInPrimary().isEmpty() && setDifference.getOnlyInSecondary().isEmpty()) {
            summary.append("Data matches completely.");
        }
        
        return summary.toString().trim();
    }
}