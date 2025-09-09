package dev.aparikh.searchdifftool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aparikh.searchdifftool.api.SearchRequest;
import dev.aparikh.searchdifftool.diff.DiffResult;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(SearchDiffController.class)
class SearchDiffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchDiffService searchDiffService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void performSearchDiff_withValidRequest_shouldReturnSuccessResponse() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
            .subject("Test Subject")
            .from("sender@example.com")
            .maxResults(100)
            .build();

        DiffResult diffResult = DiffResult.builder()
            .summary("Search diff completed successfully")
            .countsMatch(true)
            .onlyInPrimary(Collections.emptyList())
            .onlyInSecondary(Collections.emptyList())
            .contentDifferences(Collections.emptyList())
            .build();

        when(searchDiffService.performSearchDiff(any(SearchRequest.class))).thenReturn(diffResult);

        // When & Then
        mockMvc.perform(post("/api/search-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Search diff completed successfully"))
                .andExpect(jsonPath("$.data.summary").value("Search diff completed successfully"))
                .andExpect(jsonPath("$.data.countsMatch").value(true));
    }

    @Test
    void performSearchDiff_whenSearchDiffServiceThrowsException_shouldReturnErrorResponse() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
            .subject("Test")
            .build();

        SearchDiffService.SearchDiffException exception = new SearchDiffService.SearchDiffException("Service error", new RuntimeException());
        when(searchDiffService.performSearchDiff(any(SearchRequest.class))).thenThrow(exception);

        // When & Then
        mockMvc.perform(post("/api/search-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("DIFF_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void performSearchDiff_whenUnexpectedExceptionOccurs_shouldReturnGenericErrorResponse() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
            .subject("Test")
            .build();

        when(searchDiffService.performSearchDiff(any(SearchRequest.class))).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/api/search-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void health_shouldReturnHealthyStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/search-diff/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Search Diff Tool is running"))
                .andExpect(jsonPath("$.message").value("Service is healthy"));
    }

    @Test
    void performSearchDiff_withEmptyRequest_shouldReturnSuccessWithDefaults() throws Exception {
        // Given - empty request (has default values, should work)
        SearchRequest request = SearchRequest.builder().build();
        
        DiffResult diffResult = DiffResult.builder()
            .summary("Test diff completed")
            .countsMatch(true)
            .onlyInPrimary(Collections.emptyList())
            .onlyInSecondary(Collections.emptyList())
            .contentDifferences(Collections.emptyList())
            .build();
            
        when(searchDiffService.performSearchDiff(any(SearchRequest.class))).thenReturn(diffResult);

        // When & Then
        mockMvc.perform(post("/api/search-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}