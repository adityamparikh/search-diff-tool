package dev.aparikh.searchdifftool;

import dev.aparikh.searchdifftool.api.ApiResponse;
import dev.aparikh.searchdifftool.api.SearchRequest;
import dev.aparikh.searchdifftool.diff.DiffResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller providing endpoints for search diff operations.
 * 
 * <p>This controller exposes the search diff functionality through a clean REST API,
 * enabling external systems to perform comprehensive comparison analysis between
 * primary and secondary Solr systems.
 * 
 * <p>All endpoints return responses wrapped in {@link ApiResponse} for consistent
 * response structure and include comprehensive error handling with appropriate
 * HTTP status codes.
 * 
 * <h3>API Endpoints:</h3>
 * <ul>
 * <li><code>POST /api/search-diff</code> - Perform search diff analysis</li>
 * <li><code>GET /api/search-diff/health</code> - Service health check</li>
 * </ul>
 * 
 * <h3>Error Handling:</h3>
 * The controller handles various error scenarios:
 * <ul>
 * <li><strong>Validation Errors:</strong> 400 Bad Request for invalid input</li>
 * <li><strong>Search Diff Errors:</strong> 500 Internal Server Error for processing failures</li>
 * <li><strong>Unexpected Errors:</strong> 500 Internal Server Error with generic message</li>
 * </ul>
 * 
 * @author Claude Code
 * @version 1.0
 * @since 1.0
 * @see SearchRequest
 * @see DiffResult
 * @see ApiResponse
 */
@Slf4j
@RestController
@RequestMapping("/api/search-diff")
@RequiredArgsConstructor
@Validated
public class SearchDiffController {
    
    private final SearchDiffService searchDiffService;
    
    /**
     * Performs comprehensive search diff analysis between primary and secondary Solr systems.
     * 
     * <p>This endpoint orchestrates the complete diff workflow including:
     * <ul>
     * <li>Parallel search execution on both systems</li>
     * <li>Document normalization and field mapping</li>
     * <li>Hit count, result set, and content comparison</li>
     * <li>Detailed metrics and summary generation</li>
     * </ul>
     * 
     * <h3>Request Validation:</h3>
     * The request is validated using Bean Validation annotations:
     * <ul>
     * <li>At least one search criterion must be provided</li>
     * <li>maxResults must be between 1 and 10000</li>
     * <li>start must be non-negative</li>
     * <li>Date ranges must be valid if provided</li>
     * </ul>
     * 
     * <h3>Response Format:</h3>
     * Success responses include:
     * <ul>
     * <li><strong>Hit Counts:</strong> Numerical comparison between systems</li>
     * <li><strong>Result Sets:</strong> Documents unique to each system</li>
     * <li><strong>Content Differences:</strong> Field-level differences</li>
     * <li><strong>Metrics:</strong> Performance and analysis statistics</li>
     * <li><strong>Summary:</strong> Human-readable analysis summary</li>
     * </ul>
     * 
     * @param request the search request containing criteria and options
     * @return ResponseEntity containing comprehensive diff results or error information
     * @see SearchRequest for request structure and validation rules
     * @see DiffResult for complete response format
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DiffResult>> performSearchDiff(@Valid @RequestBody SearchRequest request) {
        log.info("Received search diff request: {}", request);
        
        try {
            DiffResult result = searchDiffService.performSearchDiff(request);
            
            log.info("Search diff completed successfully: {}", result.getSummary());
            return ResponseEntity.ok(ApiResponse.success(result, "Search diff completed successfully"));
            
        } catch (SearchDiffService.SearchDiffException e) {
            log.error("Error performing search diff", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("DIFF_ERROR", "Failed to perform search diff: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error performing search diff", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }
    
    /**
     * Provides a simple health check endpoint for service monitoring.
     * 
     * <p>This endpoint can be used by:
     * <ul>
     * <li>Load balancers for health checks</li>
     * <li>Monitoring systems for service availability</li>
     * <li>Development teams for quick service verification</li>
     * </ul>
     * 
     * <p>The endpoint always returns a 200 OK response when the service is running,
     * indicating that the application is started and responsive.
     * 
     * @return ResponseEntity with success message indicating service health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Search Diff Tool is running", "Service is healthy"));
    }
}