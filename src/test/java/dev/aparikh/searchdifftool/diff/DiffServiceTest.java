package dev.aparikh.searchdifftool.diff;

import dev.aparikh.searchdifftool.diff.comparison.ContentComparator;
import dev.aparikh.searchdifftool.diff.comparison.ComparisonResult;
import dev.aparikh.searchdifftool.diff.comparison.HitCountComparator;
import dev.aparikh.searchdifftool.diff.comparison.ResultSetComparator;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import dev.aparikh.searchdifftool.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffServiceTest {

    @Mock
    private ContentComparator contentComparator;
    @Mock
    private ResultSetComparator resultSetComparator;
    @Mock
    private HitCountComparator hitCountComparator;

    private DiffService diffService;

    @BeforeEach
    void setUp() {
        diffService = new DiffService(contentComparator, resultSetComparator, hitCountComparator);
    }

    @Test
    void compareDocs_withMatchingDocuments_shouldReturnEmptyDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Arrays.asList(
                createNormalizedDocument("1", "Test Subject", "test@example.com")
        );
        
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
                createNormalizedDocument("1", "Test Subject", "test@example.com")
        );

        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(Collections.emptyList())
                        .onlyInSecondary(Collections.emptyList())
                        .build());

        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs))
                .thenReturn(new ComparisonResult(
                        Collections.emptyList(),
                        0L,
                        0L,
                        Collections.emptyMap()
                ));

        // When
        DiffService.DiffAnalysis result = diffService.compareDocs(primaryDocs, secondaryDocs);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSetDifference().getOnlyInPrimary()).isEmpty();
        assertThat(result.getSetDifference().getOnlyInSecondary()).isEmpty();
        assertThat(result.getContentResult().differences()).isEmpty();
        assertThat(result.getContentResult().totalFieldDifferences()).isZero();
    }

    @Test
    void compareDocs_withDifferentDocuments_shouldReturnDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Arrays.asList(
                createNormalizedDocument("1", "Test Subject", "test1@example.com"),
                createNormalizedDocument("2", "Primary Only", "test2@example.com")
        );
        
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
                createNormalizedDocument("1", "Modified Subject", "test1@example.com"),
                createNormalizedDocument("3", "Secondary Only", "test3@example.com")
        );

        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(Collections.singletonList("2"))
                        .onlyInSecondary(Collections.singletonList("3"))
                        .build());

        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs))
                .thenReturn(new ComparisonResult(
                        Arrays.asList(),
                        1L,
                        1L,
                        Map.of("subject", 1)
                ));

        // When
        DiffService.DiffAnalysis result = diffService.compareDocs(primaryDocs, secondaryDocs);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSetDifference().getOnlyInPrimary()).hasSize(1);
        assertThat(result.getSetDifference().getOnlyInSecondary()).hasSize(1);
        assertThat(result.getContentResult().documentsWithDifferences()).isEqualTo(1L);
        assertThat(result.getContentResult().totalFieldDifferences()).isEqualTo(1L);
    }

    @Test
    void performCompleteDiff_WhenEverythingMatches_ReturnsMatchingResult() {
        // Given
        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 10L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 10L, "secondary", 100L);
        List<NormalizedDocument> primaryDocs = Arrays.asList(createDocument("1"), createDocument("2"));
        List<NormalizedDocument> secondaryDocs = Arrays.asList(createDocument("1"), createDocument("2"));

        DiffResult.HitCounts hitCounts = new DiffResult.HitCounts(10, 10, 0);
        when(hitCountComparator.compare(primaryResult, secondaryResult)).thenReturn(hitCounts);

        ResultSetComparator.SetDifference setDifference = ResultSetComparator.SetDifference.builder()
            .onlyInPrimary(Collections.emptyList())
            .onlyInSecondary(Collections.emptyList())
            .build();
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs)).thenReturn(setDifference);

        ComparisonResult contentResult = new ComparisonResult(
            Collections.emptyList(), 0, 0, new HashMap<>()
        );
        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs)).thenReturn(contentResult);

        // When
        DiffResult result = diffService.performCompleteDiffWithNormalizedDocs(
            primaryResult, secondaryResult, primaryDocs, secondaryDocs
        );

        // Then
        assertThat(result.isCountsMatch()).isTrue();
        assertThat(result.getOnlyInPrimary()).isEmpty();
        assertThat(result.getOnlyInSecondary()).isEmpty();
        assertThat(result.getContentDifferences()).isEmpty();
        assertThat(result.getSummary()).contains("Data matches completely");
    }

    @Test
    void performCompleteDiff_WhenHitCountsDiffer_ReturnsAppropriateResult() {
        // Given
        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 15L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 10L, "secondary", 100L);
        List<NormalizedDocument> primaryDocs = Arrays.asList(createDocument("1"), createDocument("2"));
        List<NormalizedDocument> secondaryDocs = Arrays.asList(createDocument("1"), createDocument("2"));

        DiffResult.HitCounts hitCounts = new DiffResult.HitCounts(15, 10, 5);
        when(hitCountComparator.compare(primaryResult, secondaryResult)).thenReturn(hitCounts);

        ResultSetComparator.SetDifference setDifference = ResultSetComparator.SetDifference.builder()
            .onlyInPrimary(Collections.emptyList())
            .onlyInSecondary(Collections.emptyList())
            .build();
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs)).thenReturn(setDifference);

        ComparisonResult contentResult = new ComparisonResult(
            Collections.emptyList(), 0, 0, new HashMap<>()
        );
        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs)).thenReturn(contentResult);

        // When
        DiffResult result = diffService.performCompleteDiffWithNormalizedDocs(
            primaryResult, secondaryResult, primaryDocs, secondaryDocs
        );

        // Then
        assertThat(result.isCountsMatch()).isFalse();
        assertThat(result.getHitCounts().getDifference()).isEqualTo(5);
        assertThat(result.getSummary()).contains("Hit counts differ by 5");
    }

    @Test
    void performCompleteDiff_WhenContentDiffers_ReturnsContentDifferences() {
        // Given
        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 2L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 2L, "secondary", 100L);
        List<NormalizedDocument> primaryDocs = Arrays.asList(createDocument("1"), createDocument("2"));
        List<NormalizedDocument> secondaryDocs = Arrays.asList(createDocument("1"), createDocument("2"));

        DiffResult.HitCounts hitCounts = new DiffResult.HitCounts(2, 2, 0);
        when(hitCountComparator.compare(primaryResult, secondaryResult)).thenReturn(hitCounts);

        ResultSetComparator.SetDifference setDifference = ResultSetComparator.SetDifference.builder()
            .onlyInPrimary(Collections.emptyList())
            .onlyInSecondary(Collections.emptyList())
            .build();
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs)).thenReturn(setDifference);

        List<DiffResult.ContentDifference> differences = Arrays.asList(
            createContentDifference("1", "title", "Old Title", "New Title")
        );
        Map<String, Integer> differencesByField = new HashMap<>();
        differencesByField.put("title", 1);

        ComparisonResult contentResult = new ComparisonResult(
            differences, 1, 1, differencesByField
        );
        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs)).thenReturn(contentResult);

        // When
        DiffResult result = diffService.performCompleteDiffWithNormalizedDocs(
            primaryResult, secondaryResult, primaryDocs, secondaryDocs
        );

        // Then
        assertThat(result.isCountsMatch()).isTrue();
        assertThat(result.getContentDifferences()).hasSize(1);
        assertThat(result.getMetrics().getTotalFieldDifferences()).isEqualTo(1);
        assertThat(result.getMetrics().getDocumentsWithDifferences()).isEqualTo(1);
        assertThat(result.getSummary()).contains("Found 1 field differences across 1 documents");
    }

    @Test
    void performCompleteDiff_WhenDocumentsInOnlyOneSet_ReturnsDifferences() {
        // Given
        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 3L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 2L, "secondary", 100L);
        List<NormalizedDocument> primaryDocs = Arrays.asList(
            createDocument("1"), createDocument("2"), createDocument("3")
        );
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
            createDocument("1"), createDocument("2")
        );

        DiffResult.HitCounts hitCounts = new DiffResult.HitCounts(3, 2, 1);
        when(hitCountComparator.compare(primaryResult, secondaryResult)).thenReturn(hitCounts);

        ResultSetComparator.SetDifference setDifference = ResultSetComparator.SetDifference.builder()
            .onlyInPrimary(Arrays.asList("3"))
            .onlyInSecondary(Collections.emptyList())
            .build();
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs)).thenReturn(setDifference);

        ComparisonResult contentResult = new ComparisonResult(
            Collections.emptyList(), 0, 0, new HashMap<>()
        );
        when(contentComparator.compareDocuments(any(), any())).thenReturn(contentResult);

        // When
        DiffResult result = diffService.performCompleteDiffWithNormalizedDocs(
            primaryResult, secondaryResult, primaryDocs, secondaryDocs
        );

        // Then
        assertThat(result.isCountsMatch()).isFalse();
        assertThat(result.getOnlyInPrimary()).hasSize(1);
        assertThat(result.getOnlyInSecondary()).isEmpty();
        assertThat(result.getSummary()).contains("1 documents only in primary");
    }

    private NormalizedDocument createNormalizedDocument(String id, String subject, String from) {
        return new NormalizedDocument(
                id,
                subject,
                from,
                "recipient@example.com",
                "Test body",
                LocalDateTime.now(),
                Collections.emptyList(),
                Collections.emptyMap(),
                "test"
        );
    }

    private NormalizedDocument createDocument(String id) {
        return NormalizedDocument.builder()
            .id(id)
            .subject("Test Document " + id)
            .from("test@example.com")
            .to("recipient@example.com")
            .body("Test body")
            .date(LocalDateTime.now())
            .attachments(Collections.emptyList())
            .metadata(Collections.emptyMap())
            .source("test")
            .build();
    }

    private DiffResult.ContentDifference createContentDifference(
            String docId, String field, String primaryValue, String secondaryValue) {
        return DiffResult.ContentDifference.builder()
            .documentId(docId)
            .field(field)
            .primaryValue(primaryValue)
            .secondaryValue(secondaryValue)
            .build();
    }
}

