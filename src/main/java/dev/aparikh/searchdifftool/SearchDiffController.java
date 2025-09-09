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

@Slf4j
@RestController
@RequestMapping("/api/search-diff")
@RequiredArgsConstructor
@Validated
public class SearchDiffController {
    
    private final SearchDiffService searchDiffService;
    
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
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Search Diff Tool is running", "Service is healthy"));
    }
}