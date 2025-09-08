package dev.aparikh.searchdifftool.api;

import dev.aparikh.searchdifftool.api.model.ApiResponse;
import dev.aparikh.searchdifftool.api.model.SearchRequest;
import dev.aparikh.searchdifftool.diff.DiffService;
import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.email.model.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/search-diff")
@RequiredArgsConstructor
@Validated
public class SearchDiffController {
    
    private final DiffService diffService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<DiffResult>> performSearchDiff(@Valid @RequestBody SearchRequest request) {
        log.info("Received search diff request: {}", request);
        
        try {
            SearchCriteria criteria = mapToSearchCriteria(request);
            DiffResult result = diffService.performDiff(criteria);
            
            log.info("Search diff completed successfully: {}", result.getSummary());
            return ResponseEntity.ok(ApiResponse.success(result, "Search diff completed successfully"));
            
        } catch (Exception e) {
            log.error("Error performing search diff", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("DIFF_ERROR", "Failed to perform search diff: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Search Diff Tool is running", "Service is healthy"));
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
}