package dev.aparikh.searchdifftool.diff;

import dev.aparikh.searchdifftool.diff.comparison.ContentComparator;
import dev.aparikh.searchdifftool.diff.comparison.HitCountComparator;
import dev.aparikh.searchdifftool.diff.comparison.ResultSetComparator;
import dev.aparikh.searchdifftool.diff.model.DiffResult;
import dev.aparikh.searchdifftool.email.EmailSearchService;
import dev.aparikh.searchdifftool.email.model.EmailDocument;
import dev.aparikh.searchdifftool.email.model.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffServiceTest {

    @Mock
    private EmailSearchService emailSearchService;

    @Mock
    private HitCountComparator hitCountComparator;

    @Mock
    private ContentComparator contentComparator;

    @Mock
    private ResultSetComparator resultSetComparator;

    private DiffService diffService;

    @BeforeEach
    void setUp() {
        diffService = new DiffService(emailSearchService, hitCountComparator, contentComparator, resultSetComparator);
    }

    @Test
    void performDiff_withMatchingResults_shouldReturnSuccessfulDiff() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .maxResults(10)
                .build();

        List<EmailDocument> primaryDocs = Arrays.asList(
                createEmailDocument("1", "Test Subject", "Primary"),
                createEmailDocument("2", "Another Subject", "Primary")
        );

        List<EmailDocument> secondaryDocs = Arrays.asList(
                createEmailDocument("1", "Test Subject", "Secondary"),
                createEmailDocument("2", "Another Subject", "Secondary")
        );

        EmailSearchService.SearchResult primaryResult = EmailSearchService.SearchResult.builder()
                .documents(primaryDocs)
                .totalHits(2L)
                .source("primary")
                .queryTime(100L)
                .build();

        EmailSearchService.SearchResult secondaryResult = EmailSearchService.SearchResult.builder()
                .documents(secondaryDocs)
                .totalHits(2L)
                .source("secondary")
                .queryTime(120L)
                .build();

        // Mock dependencies
        when(emailSearchService.searchPrimary(criteria)).thenReturn(primaryResult);
        when(emailSearchService.searchSecondary(criteria)).thenReturn(secondaryResult);

        when(hitCountComparator.compare(primaryResult, secondaryResult))
                .thenReturn(DiffResult.HitCounts.builder()
                        .primary(2L)
                        .secondary(2L)
                        .difference(0L)
                        .build());

        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(Collections.emptyList())
                        .onlyInSecondary(Collections.emptyList())
                        .build());

        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs))
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(Collections.emptyList())
                        .documentsWithDifferences(0L)
                        .totalFieldDifferences(0L)
                        .differencesByField(Collections.emptyMap())
                        .build());

        // When
        DiffResult result = diffService.performDiff(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHitCounts().getPrimary()).isEqualTo(2L);
        assertThat(result.getHitCounts().getSecondary()).isEqualTo(2L);
        assertThat(result.isCountsMatch()).isTrue();
        assertThat(result.getOnlyInPrimary()).isEmpty();
        assertThat(result.getOnlyInSecondary()).isEmpty();
        assertThat(result.getContentDifferences()).isEmpty();
        assertThat(result.getSummary()).contains("Data matches completely");
    }

    @Test
    void performDiff_withDifferentHitCounts_shouldReturnDifferences() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .build();

        EmailSearchService.SearchResult primaryResult = EmailSearchService.SearchResult.builder()
                .documents(Collections.emptyList())
                .totalHits(5L)
                .source("primary")
                .queryTime(100L)
                .build();

        EmailSearchService.SearchResult secondaryResult = EmailSearchService.SearchResult.builder()
                .documents(Collections.emptyList())
                .totalHits(3L)
                .source("secondary")
                .queryTime(120L)
                .build();

        when(emailSearchService.searchPrimary(criteria)).thenReturn(primaryResult);
        when(emailSearchService.searchSecondary(criteria)).thenReturn(secondaryResult);

        when(hitCountComparator.compare(primaryResult, secondaryResult))
                .thenReturn(DiffResult.HitCounts.builder()
                        .primary(5L)
                        .secondary(3L)
                        .difference(2L)
                        .build());

        when(resultSetComparator.findDifferences(any(), any()))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(Collections.emptyList())
                        .onlyInSecondary(Collections.emptyList())
                        .build());

        when(contentComparator.compareDocuments(any(), any()))
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(Collections.emptyList())
                        .documentsWithDifferences(0L)
                        .totalFieldDifferences(0L)
                        .differencesByField(Collections.emptyMap())
                        .build());

        // When
        DiffResult result = diffService.performDiff(criteria);

        // Then
        assertThat(result.isCountsMatch()).isFalse();
        assertThat(result.getHitCounts().getDifference()).isEqualTo(2L);
        assertThat(result.getSummary()).contains("Hit counts differ by 2");
    }

    @Test
    void performDiff_withSameHitCountsButDifferentContent_shouldReturnContentDifferences() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .build();

        List<EmailDocument> primaryDocs = Arrays.asList(
                createEmailDocument("1", "Test Subject", "primary@example.com", "Primary content"),
                createEmailDocument("2", "Another Subject", "sender@example.com", "Primary body")
        );

        List<EmailDocument> secondaryDocs = Arrays.asList(
                createEmailDocument("1", "Test Subject MODIFIED", "primary@example.com", "Secondary content"),
                createEmailDocument("2", "Another Subject", "different@example.com", "Secondary body")
        );

        EmailSearchService.SearchResult primaryResult = EmailSearchService.SearchResult.builder()
                .documents(primaryDocs)
                .totalHits(2L)
                .source("primary")
                .queryTime(100L)
                .build();

        EmailSearchService.SearchResult secondaryResult = EmailSearchService.SearchResult.builder()
                .documents(secondaryDocs)
                .totalHits(2L)
                .source("secondary")
                .queryTime(120L)
                .build();

        // Mock hit counts as matching
        when(emailSearchService.searchPrimary(criteria)).thenReturn(primaryResult);
        when(emailSearchService.searchSecondary(criteria)).thenReturn(secondaryResult);

        when(hitCountComparator.compare(primaryResult, secondaryResult))
                .thenReturn(DiffResult.HitCounts.builder()
                        .primary(2L)
                        .secondary(2L)
                        .difference(0L)
                        .build());

        // No missing documents
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(Collections.emptyList())
                        .onlyInSecondary(Collections.emptyList())
                        .build());

        // Mock content differences
        List<DiffResult.ContentDifference> contentDiffs = Arrays.asList(
                DiffResult.ContentDifference.builder()
                        .documentId("1")
                        .field("subject")
                        .primaryValue("Test Subject")
                        .secondaryValue("Test Subject MODIFIED")
                        .differenceType("DIFFERENT")
                        .build(),
                DiffResult.ContentDifference.builder()
                        .documentId("1")
                        .field("body")
                        .primaryValue("Primary content")
                        .secondaryValue("Secondary content")
                        .differenceType("DIFFERENT")
                        .build(),
                DiffResult.ContentDifference.builder()
                        .documentId("2")
                        .field("from")
                        .primaryValue("sender@example.com")
                        .secondaryValue("different@example.com")
                        .differenceType("DIFFERENT")
                        .build()
        );

        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs))
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(contentDiffs)
                        .documentsWithDifferences(2L)
                        .totalFieldDifferences(3L)
                        .differencesByField(Map.of("subject", 1, "body", 1, "from", 1))
                        .build());

        // When
        DiffResult result = diffService.performDiff(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHitCounts().getPrimary()).isEqualTo(2L);
        assertThat(result.getHitCounts().getSecondary()).isEqualTo(2L);
        assertThat(result.isCountsMatch()).isTrue();
        assertThat(result.getOnlyInPrimary()).isEmpty();
        assertThat(result.getOnlyInSecondary()).isEmpty();
        assertThat(result.getContentDifferences()).hasSize(3);
        assertThat(result.getSummary()).contains("Hit counts match (2)")
                .contains("Found 3 field differences across 2 documents");
    }

    @Test
    void performDiff_withDifferentHitCountsAndExtraResultsOnly_shouldReturnMissingDocuments() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .build();

        List<EmailDocument> primaryDocs = Arrays.asList(
                createEmailDocument("1", "Common Subject", "test@example.com", "Common content"),
                createEmailDocument("2", "Another Common", "test@example.com", "Another content"),
                createEmailDocument("3", "Only in Primary", "primary@example.com", "Primary only content")
        );

        List<EmailDocument> secondaryDocs = Arrays.asList(
                createEmailDocument("1", "Common Subject", "test@example.com", "Common content"),
                createEmailDocument("2", "Another Common", "test@example.com", "Another content")
        );

        EmailSearchService.SearchResult primaryResult = EmailSearchService.SearchResult.builder()
                .documents(primaryDocs)
                .totalHits(3L)
                .source("primary")
                .queryTime(100L)
                .build();

        EmailSearchService.SearchResult secondaryResult = EmailSearchService.SearchResult.builder()
                .documents(secondaryDocs)
                .totalHits(2L)
                .source("secondary")
                .queryTime(120L)
                .build();

        when(emailSearchService.searchPrimary(criteria)).thenReturn(primaryResult);
        when(emailSearchService.searchSecondary(criteria)).thenReturn(secondaryResult);

        when(hitCountComparator.compare(primaryResult, secondaryResult))
                .thenReturn(DiffResult.HitCounts.builder()
                        .primary(3L)
                        .secondary(2L)
                        .difference(1L)
                        .build());

        // Mock missing document in secondary
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(List.of("3"))
                        .onlyInSecondary(Collections.emptyList())
                        .build());

        // No content differences for common documents
        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs))
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(Collections.emptyList())
                        .documentsWithDifferences(0L)
                        .totalFieldDifferences(0L)
                        .differencesByField(Collections.emptyMap())
                        .build());

        // When
        DiffResult result = diffService.performDiff(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCountsMatch()).isFalse();
        assertThat(result.getHitCounts().getDifference()).isEqualTo(1L);
        assertThat(result.getOnlyInPrimary()).hasSize(1).contains("3");
        assertThat(result.getOnlyInSecondary()).isEmpty();
        assertThat(result.getContentDifferences()).isEmpty();
        assertThat(result.getSummary()).contains("Hit counts differ by 1")
                .contains("1 documents only in primary");
    }

    @Test
    void performDiff_withDifferentHitCountsExtraResultsAndContentDifferences_shouldReturnAllDifferences() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .build();

        List<EmailDocument> primaryDocs = Arrays.asList(
                createEmailDocument("1", "Modified Subject", "primary@example.com", "Modified content"),
                createEmailDocument("2", "Common Subject", "common@example.com", "Common content"),
                createEmailDocument("3", "Only in Primary", "primary@example.com", "Primary only")
        );

        List<EmailDocument> secondaryDocs = Arrays.asList(
                createEmailDocument("1", "Different Subject", "secondary@example.com", "Different content"),
                createEmailDocument("2", "Common Subject", "common@example.com", "Common content"),
                createEmailDocument("4", "Only in Secondary", "secondary@example.com", "Secondary only")
        );

        EmailSearchService.SearchResult primaryResult = EmailSearchService.SearchResult.builder()
                .documents(primaryDocs)
                .totalHits(3L)
                .source("primary")
                .queryTime(100L)
                .build();

        EmailSearchService.SearchResult secondaryResult = EmailSearchService.SearchResult.builder()
                .documents(secondaryDocs)
                .totalHits(3L)
                .source("secondary")
                .queryTime(120L)
                .build();

        when(emailSearchService.searchPrimary(criteria)).thenReturn(primaryResult);
        when(emailSearchService.searchSecondary(criteria)).thenReturn(secondaryResult);

        // Hit counts are same but documents are different
        when(hitCountComparator.compare(primaryResult, secondaryResult))
                .thenReturn(DiffResult.HitCounts.builder()
                        .primary(3L)
                        .secondary(3L)
                        .difference(0L)
                        .build());

        // Mock documents only in each system
        when(resultSetComparator.findDifferences(primaryDocs, secondaryDocs))
                .thenReturn(ResultSetComparator.SetDifference.builder()
                        .onlyInPrimary(List.of("3"))
                        .onlyInSecondary(List.of("4"))
                        .build());

        // Mock content differences for common documents
        List<DiffResult.ContentDifference> contentDiffs = Arrays.asList(
                DiffResult.ContentDifference.builder()
                        .documentId("1")
                        .field("subject")
                        .primaryValue("Modified Subject")
                        .secondaryValue("Different Subject")
                        .differenceType("DIFFERENT")
                        .build(),
                DiffResult.ContentDifference.builder()
                        .documentId("1")
                        .field("from")
                        .primaryValue("primary@example.com")
                        .secondaryValue("secondary@example.com")
                        .differenceType("DIFFERENT")
                        .build(),
                DiffResult.ContentDifference.builder()
                        .documentId("1")
                        .field("body")
                        .primaryValue("Modified content")
                        .secondaryValue("Different content")
                        .differenceType("DIFFERENT")
                        .build()
        );

        when(contentComparator.compareDocuments(primaryDocs, secondaryDocs))
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(contentDiffs)
                        .documentsWithDifferences(1L)
                        .totalFieldDifferences(3L)
                        .differencesByField(Map.of("subject", 1, "from", 1, "body", 1))
                        .build());

        // When
        DiffResult result = diffService.performDiff(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCountsMatch()).isTrue(); // Same hit count
        assertThat(result.getOnlyInPrimary()).hasSize(1).contains("3");
        assertThat(result.getOnlyInSecondary()).hasSize(1).contains("4");
        assertThat(result.getContentDifferences()).hasSize(3);
        assertThat(result.getSummary())
                .contains("Hit counts match (3)")
                .contains("1 documents only in primary")
                .contains("1 documents only in secondary")
                .contains("Found 3 field differences across 1 documents");
    }

    private EmailDocument createEmailDocument(String id, String subject, String source) {
        return createEmailDocument(id, subject, "test@example.com", "Test email body", source);
    }

    private EmailDocument createEmailDocument(String id, String subject, String from, String body) {
        return createEmailDocument(id, subject, from, body, "primary");
    }

    private EmailDocument createEmailDocument(String id, String subject, String from, String body, String source) {
        return EmailDocument.builder()
                .id(id)
                .subject(subject)
                .from(from)
                .to("recipient@example.com")
                .body(body)
                .date(LocalDateTime.now())
                .source(source.toLowerCase())
                .build();
    }
}