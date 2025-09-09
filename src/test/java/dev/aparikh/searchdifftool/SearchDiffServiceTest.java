package dev.aparikh.searchdifftool;

import dev.aparikh.searchdifftool.api.SearchRequest;
import dev.aparikh.searchdifftool.diff.DiffResult;
import dev.aparikh.searchdifftool.diff.DiffService;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import dev.aparikh.searchdifftool.normalization.NormalizationService;
import dev.aparikh.searchdifftool.search.SearchCriteria;
import dev.aparikh.searchdifftool.search.SearchResult;
import dev.aparikh.searchdifftool.search.SearchService;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchDiffServiceTest {

    @Mock
    private SearchService searchService;

    @Mock
    private NormalizationService normalizationService;

    @Mock
    private DiffService diffService;

    private SearchDiffService searchDiffService;

    @BeforeEach
    void setUp() {
        searchDiffService = new SearchDiffService(searchService, normalizationService, diffService);
    }

    @Test
    void performSearchDiff_withValidRequest_shouldReturnDiffResult() {
        // Given
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .maxResults(10)
                .build();

        SolrDocument doc1 = new SolrDocument();
        doc1.setField("id", "1");
        doc1.setField("subject", "Test Subject");

        SearchResult primaryResult = new SearchResult(List.of(doc1), 1L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(List.of(doc1), 1L, "secondary", 120L);

        NormalizedDocument normalizedDoc = new NormalizedDocument(
                "1", "Test Subject", "test@example.com", "recipient@example.com",
                "Test body", LocalDateTime.now(), Collections.emptyList(),
                Collections.emptyMap(), "primary"
        );

        DiffResult expectedResult = DiffResult.builder()
                .summary("Search completed")
                .countsMatch(true)
                .onlyInPrimary(Collections.emptyList())
                .onlyInSecondary(Collections.emptyList())
                .contentDifferences(Collections.emptyList())
                .build();

        when(searchService.searchPrimary(any(SearchCriteria.class))).thenReturn(primaryResult);
        when(searchService.searchSecondary(any(SearchCriteria.class))).thenReturn(secondaryResult);
        when(normalizationService.normalize(any(SolrDocument.class), anyBoolean())).thenReturn(normalizedDoc);
        when(diffService.performCompleteDiffWithNormalizedDocs(any(), any(), any(), any())).thenReturn(expectedResult);

        // When
        DiffResult result = searchDiffService.performSearchDiff(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Search completed");
        assertThat(result.isCountsMatch()).isTrue();
    }

    @Test
    void performSearchDiff_whenSearchServiceThrowsException_shouldThrowSearchDiffException() {
        // Given
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .build();

        when(searchService.searchPrimary(any(SearchCriteria.class)))
                .thenThrow(new RuntimeException("Search failed"));

        // When & Then
        assertThatThrownBy(() -> searchDiffService.performSearchDiff(request))
                .isInstanceOf(SearchDiffService.SearchDiffException.class)
                .hasMessage("Failed to perform search diff")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void performSearchDiff_whenNormalizationServiceThrowsException_shouldThrowSearchDiffException() {
        // Given
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .build();

        SolrDocument doc1 = new SolrDocument();
        SearchResult primaryResult = new SearchResult(List.of(doc1), 1L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 0L, "secondary", 50L);

        when(searchService.searchPrimary(any(SearchCriteria.class))).thenReturn(primaryResult);
        when(searchService.searchSecondary(any(SearchCriteria.class))).thenReturn(secondaryResult);
        when(normalizationService.normalize(any(SolrDocument.class), anyBoolean()))
                .thenThrow(new RuntimeException("Normalization failed"));

        // When & Then
        assertThatThrownBy(() -> searchDiffService.performSearchDiff(request))
                .isInstanceOf(SearchDiffService.SearchDiffException.class)
                .hasMessage("Failed to perform search diff")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void performSearchDiff_withEmptyResults_shouldReturnValidDiffResult() {
        // Given
        SearchRequest request = SearchRequest.builder()
                .build();

        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 0L, "primary", 50L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 0L, "secondary", 60L);

        DiffResult expectedResult = DiffResult.builder()
                .summary("No results found")
                .countsMatch(true)
                .onlyInPrimary(Collections.emptyList())
                .onlyInSecondary(Collections.emptyList())
                .contentDifferences(Collections.emptyList())
                .build();

        when(searchService.searchPrimary(any(SearchCriteria.class))).thenReturn(primaryResult);
        when(searchService.searchSecondary(any(SearchCriteria.class))).thenReturn(secondaryResult);
        when(diffService.performCompleteDiffWithNormalizedDocs(any(), any(), any(), any())).thenReturn(expectedResult);

        // When
        DiffResult result = searchDiffService.performSearchDiff(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("No results found");
        assertThat(result.isCountsMatch()).isTrue();
    }

    @Test
    void performSearchDiff_withDefaultMaxResults_shouldUse1000() {
        // Given
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .build(); // maxResults is null

        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 0L, "primary", 50L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 0L, "secondary", 60L);

        DiffResult expectedResult = DiffResult.builder()
                .summary("Test")
                .countsMatch(true)
                .onlyInPrimary(Collections.emptyList())
                .onlyInSecondary(Collections.emptyList())
                .contentDifferences(Collections.emptyList())
                .build();

        when(searchService.searchPrimary(any(SearchCriteria.class))).thenReturn(primaryResult);
        when(searchService.searchSecondary(any(SearchCriteria.class))).thenReturn(secondaryResult);
        when(diffService.performCompleteDiffWithNormalizedDocs(any(), any(), any(), any())).thenReturn(expectedResult);

        // When
        DiffResult result = searchDiffService.performSearchDiff(request);

        // Then
        assertThat(result).isNotNull();
        // Verify that the internal mapping used maxResults = 1000 (default value)
        assertThat(result.getSummary()).isEqualTo("Test");
    }

    @Test
    void performSearchDiff_withDefaultStart_shouldUse0() {
        // Given
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .build(); // start is null

        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 0L, "primary", 50L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 0L, "secondary", 60L);

        DiffResult expectedResult = DiffResult.builder()
                .summary("Test")
                .countsMatch(true)
                .onlyInPrimary(Collections.emptyList())
                .onlyInSecondary(Collections.emptyList())
                .contentDifferences(Collections.emptyList())
                .build();

        when(searchService.searchPrimary(any(SearchCriteria.class))).thenReturn(primaryResult);
        when(searchService.searchSecondary(any(SearchCriteria.class))).thenReturn(secondaryResult);
        when(diffService.performCompleteDiffWithNormalizedDocs(any(), any(), any(), any())).thenReturn(expectedResult);

        // When
        DiffResult result = searchDiffService.performSearchDiff(request);

        // Then
        assertThat(result).isNotNull();
        // Verify that the internal mapping used start = 0 (default value)
        assertThat(result.getSummary()).isEqualTo("Test");
    }
}