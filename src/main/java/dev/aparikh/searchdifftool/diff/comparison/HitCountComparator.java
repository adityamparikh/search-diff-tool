package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.email.EmailSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HitCountComparator {
    
    public DiffResult.HitCounts compare(EmailSearchService.SearchResult primaryResult, 
                                       EmailSearchService.SearchResult secondaryResult) {
        
        long primaryCount = primaryResult.getTotalHits();
        long secondaryCount = secondaryResult.getTotalHits();
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