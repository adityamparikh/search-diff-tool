package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.DiffResult;
import dev.aparikh.searchdifftool.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Component responsible for comparing total hit counts between primary and secondary search results.
 * 
 * <p>This comparator provides a high-level quantitative analysis by comparing the total number
 * of documents returned by each search system. It's typically the first comparison performed
 * in the diff analysis workflow as it provides immediate insight into whether the systems
 * return similar volumes of results.
 * 
 * <p>Key features:
 * <ul>
 * <li><strong>Null Safety:</strong> Handles null search results gracefully (treats as 0 hits)</li>
 * <li><strong>Difference Calculation:</strong> Computes the numerical difference (primary - secondary)</li>
 * <li><strong>Logging:</strong> Provides detailed logging of comparison results</li>
 * </ul>
 * 
 * <p>The comparison result includes both raw hit counts and the calculated difference,
 * enabling easy analysis of quantitative discrepancies between systems.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 * @see SearchResult
 * @see DiffResult.HitCounts
 */
@Slf4j
@Component
public class HitCountComparator {
    
    /**
     * Compares the total hit counts between primary and secondary search results.
     * 
     * <p>This method safely handles null results by treating them as having zero hits.
     * The difference is calculated as (primary - secondary), so:
     * <ul>
     * <li>Positive difference: Primary has more hits than secondary</li>
     * <li>Negative difference: Secondary has more hits than primary</li>
     * <li>Zero difference: Both systems returned the same number of hits</li>
     * </ul>
     * 
     * @param primaryResult search result from primary system (null-safe)
     * @param secondaryResult search result from secondary system (null-safe)
     * @return hit count comparison including individual counts and calculated difference
     */
    public DiffResult.HitCounts compare(SearchResult primaryResult,
                                        SearchResult secondaryResult) {
        
        long primaryCount = primaryResult != null ? primaryResult.totalHits() : 0;
        long secondaryCount = secondaryResult != null ? secondaryResult.totalHits() : 0;
        long difference = primaryCount - secondaryCount;
        
        log.info("Hit count comparison - Primary: {}, Secondary: {}, Difference: {}", 
            primaryCount, secondaryCount, difference);
        
        return DiffResult.HitCounts.builder()
            .primary(primaryCount)
            .secondary(secondaryCount)
            .difference(difference)
            .build();
    }
}