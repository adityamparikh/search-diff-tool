package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HitCountComparator {
    
    public DiffResult.HitCounts compare(SearchResult primaryResult,
                                        SearchResult secondaryResult) {
        
        long primaryCount = primaryResult.totalHits();
        long secondaryCount = secondaryResult.totalHits();
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