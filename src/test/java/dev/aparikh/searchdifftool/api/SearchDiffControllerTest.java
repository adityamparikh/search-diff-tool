package dev.aparikh.searchdifftool.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aparikh.searchdifftool.api.model.SearchRequest;
import dev.aparikh.searchdifftool.diff.DiffService;
import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.email.model.SearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchDiffController.class)
class SearchDiffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DiffService diffService;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/search-diff/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Search Diff Tool is running"));
    }

    @Test
    void testSearchDiffEndpoint() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .maxResults(10)
                .build();

        DiffResult mockResult = DiffResult.builder()
                .hitCounts(DiffResult.HitCounts.builder()
                    .primary(5L)
                    .secondary(5L)
                    .difference(0L)
                    .build())
                .countsMatch(true)
                .onlyInPrimary(Collections.emptyList())
                .onlyInSecondary(Collections.emptyList())
                .contentDifferences(Collections.emptyList())
                .summary("Hit counts match (5). Data matches completely.")
                .metrics(DiffResult.ComparisonMetrics.builder()
                    .totalDocumentsCompared(5L)
                    .documentsWithDifferences(0L)
                    .totalFieldDifferences(0L)
                    .queryTimePrimary(100L)
                    .queryTimeSecondary(120L)
                    .comparisonTimeMs(50L)
                    .differencesByField(Collections.emptyMap())
                    .build())
                .build();

        when(diffService.performDiff(any(SearchCriteria.class))).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/api/search-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hitCounts.primary").value(5))
                .andExpect(jsonPath("$.data.hitCounts.secondary").value(5))
                .andExpect(jsonPath("$.data.countsMatch").value(true))
                .andExpect(jsonPath("$.data.summary").value("Hit counts match (5). Data matches completely."));
    }

    @Test
    void testSearchDiffWithInvalidRequest() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .maxResults(-1) // Invalid max results
                .build();

        mockMvc.perform(post("/api/search-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}