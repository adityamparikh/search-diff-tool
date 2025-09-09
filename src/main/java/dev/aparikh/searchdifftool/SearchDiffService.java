package dev.aparikh.searchdifftool;

import dev.aparikh.searchdifftool.api.SearchRequest;
import dev.aparikh.searchdifftool.diff.DiffService;
import dev.aparikh.searchdifftool.diff.DiffResult;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import dev.aparikh.searchdifftool.normalization.NormalizationService;
import dev.aparikh.searchdifftool.search.SearchCriteria;
import dev.aparikh.searchdifftool.search.SearchResult;
import dev.aparikh.searchdifftool.search.SearchService;
import org.apache.solr.common.SolrDocument;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchDiffService {
    
    private final SearchService searchService;
    private final NormalizationService normalizationService;
    private final DiffService diffService;
    
    public DiffResult performSearchDiff(SearchRequest request) {
        log.info("Processing search diff request: {}", request);
        
        try {
            SearchCriteria criteria = mapToSearchCriteria(request);
            
            // Step 1: Perform searches on both systems using SearchService
            SearchResult primaryResult = searchService.searchPrimary(criteria);
            SearchResult secondaryResult = searchService.searchSecondary(criteria);
            
            log.info("Primary search returned {} hits, Secondary search returned {} hits", 
                primaryResult.totalHits(), secondaryResult.totalHits());
            
            // Step 2: Normalize the search results
            List<NormalizedDocument> primaryNormalizedDocs = normalizeDocuments(primaryResult.documents(), true);
            List<NormalizedDocument> secondaryNormalizedDocs = normalizeDocuments(secondaryResult.documents(), false);
            
            log.info("Normalized {} primary and {} secondary documents", 
                primaryNormalizedDocs.size(), secondaryNormalizedDocs.size());
            
            // Step 3: Perform complete diff analysis using DiffService
            DiffResult result = diffService.performCompleteDiffWithNormalizedDocs(
                primaryResult, secondaryResult, primaryNormalizedDocs, secondaryNormalizedDocs);
            
            log.info("Search diff completed successfully: {}", result.getSummary());
            return result;
            
        } catch (Exception e) {
            log.error("Error performing search diff for request: {}", request, e);
            throw new SearchDiffException("Failed to perform search diff", e);
        }
    }
    
    private List<NormalizedDocument> normalizeDocuments(List<SolrDocument> solrDocuments, boolean isPrimary) {
        List<NormalizedDocument> normalizedDocuments = new ArrayList<>();
        
        for (SolrDocument doc : solrDocuments) {
            NormalizedDocument normalizedDoc = normalizationService.normalize(doc, isPrimary);
            normalizedDocuments.add(normalizedDoc);
        }
        
        return normalizedDocuments;
    }
    
    private SearchCriteria mapToSearchCriteria(SearchRequest request) {
        return SearchCriteria.builder()
            .subject(request.getSubject())
            .from(request.getFrom())
            .to(request.getTo())
            .body(request.getBody())
            .dateFrom(request.getDateFrom())
            .dateTo(request.getDateTo())
            .attachmentName(request.getAttachmentName())
            .query(request.getQuery())
            .maxResults(request.getMaxResults() != null ? request.getMaxResults() : 1000)
            .start(request.getStart() != null ? request.getStart() : 0)
            .filters(request.getFilters())
            .build();
    }
    
    public static class SearchDiffException extends RuntimeException {
        public SearchDiffException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}