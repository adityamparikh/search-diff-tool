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

/**
 * Main orchestration service for performing search diff operations between primary and secondary Solr systems.
 * 
 * <p>This service coordinates the entire search diff workflow:
 * <ol>
 * <li>Executes parallel searches on both primary and secondary Solr instances</li>
 * <li>Normalizes the retrieved documents to a common format</li>
 * <li>Performs comprehensive diff analysis including hit count, result set, and content comparisons</li>
 * <li>Returns detailed diff results with analysis metrics</li>
 * </ol>
 * 
 * <p>The service handles error scenarios gracefully and provides detailed logging for monitoring
 * and debugging purposes.
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchDiffService {
    
    private final SearchService searchService;
    private final NormalizationService normalizationService;
    private final DiffService diffService;
    
    /**
     * Performs a comprehensive search diff operation between primary and secondary Solr systems.
     * 
     * <p>This method orchestrates the complete diff workflow:
     * <ul>
     * <li>Maps the incoming request to internal search criteria</li>
     * <li>Executes searches on both primary and secondary systems</li>
     * <li>Normalizes all retrieved documents to a common format</li>
     * <li>Performs detailed comparison analysis</li>
     * </ul>
     * 
     * @param request the search request containing search criteria and options
     * @return {@link DiffResult} containing comprehensive comparison results including:
     *         hit counts, result set differences, content differences, and summary metrics
     * @throws SearchDiffException if any error occurs during the search diff process
     */
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
    
    /**
     * Normalizes a list of Solr documents to the common NormalizedDocument format.
     * 
     * @param solrDocuments the list of Solr documents to normalize
     * @param isPrimary true if documents are from primary system, false if from secondary
     * @return list of normalized documents in common format
     */
    private List<NormalizedDocument> normalizeDocuments(List<SolrDocument> solrDocuments, boolean isPrimary) {
        List<NormalizedDocument> normalizedDocuments = new ArrayList<>();
        
        for (SolrDocument doc : solrDocuments) {
            NormalizedDocument normalizedDoc = normalizationService.normalize(doc, isPrimary);
            normalizedDocuments.add(normalizedDoc);
        }
        
        return normalizedDocuments;
    }
    
    /**
     * Maps an API search request to internal search criteria format.
     * 
     * <p>Applies default values for pagination parameters if not provided:
     * <ul>
     * <li>maxResults defaults to 1000</li>
     * <li>start defaults to 0</li>
     * </ul>
     * 
     * @param request the incoming search request from API
     * @return mapped search criteria for internal processing
     */
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
    
    /**
     * Exception thrown when search diff operations fail.
     * 
     * <p>This exception wraps underlying errors that occur during the search diff process,
     * providing context about the failure while preserving the original cause.
     */
    public static class SearchDiffException extends RuntimeException {
        /**
         * Constructs a SearchDiffException with a message and underlying cause.
         * 
         * @param message descriptive error message
         * @param cause the underlying exception that caused this error
         */
        public SearchDiffException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}