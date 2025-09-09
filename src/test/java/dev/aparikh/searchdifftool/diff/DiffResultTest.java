package dev.aparikh.searchdifftool.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aparikh.searchdifftool.diff.comparison.DifferenceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiffResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builder_ShouldCreateDiffResultWithAllFields() {
        // Given
        DiffResult.HitCounts hitCounts = DiffResult.HitCounts.builder()
                .primary(100L)
                .secondary(95L)
                .difference(5L)
                .build();

        DiffResult.ContentDifference contentDiff = DiffResult.ContentDifference.builder()
                .documentId("doc1")
                .field("title")
                .primaryValue("Original Title")
                .secondaryValue("Updated Title")
                .differenceType(DifferenceType.DIFFERENT)
                .build();

        DiffResult.ComparisonMetrics metrics = DiffResult.ComparisonMetrics.builder()
                .totalDocumentsCompared(100L)
                .documentsWithDifferences(5L)
                .totalFieldDifferences(8L)
                .queryTimePrimary(150L)
                .queryTimeSecondary(160L)
                .comparisonTimeMs(25L)
                .differencesByField(Map.of("title", 3, "description", 5))
                .build();

        // When
        DiffResult result = DiffResult.builder()
                .hitCounts(hitCounts)
                .countsMatch(false)
                .onlyInPrimary(List.of("doc2", "doc3"))
                .onlyInSecondary(List.of("doc4"))
                .contentDifferences(List.of(contentDiff))
                .summary("Test summary")
                .metrics(metrics)
                .build();

        // Then
        assertThat(result.getHitCounts()).isEqualTo(hitCounts);
        assertThat(result.isCountsMatch()).isFalse();
        assertThat(result.getOnlyInPrimary()).containsExactly("doc2", "doc3");
        assertThat(result.getOnlyInSecondary()).containsExactly("doc4");
        assertThat(result.getContentDifferences()).containsExactly(contentDiff);
        assertThat(result.getSummary()).isEqualTo("Test summary");
        assertThat(result.getMetrics()).isEqualTo(metrics);
    }

    @Test
    void hitCounts_ShouldCreateWithBuilder() {
        // When
        DiffResult.HitCounts hitCounts = DiffResult.HitCounts.builder()
                .primary(50L)
                .secondary(45L)
                .difference(5L)
                .build();

        // Then
        assertThat(hitCounts.getPrimary()).isEqualTo(50L);
        assertThat(hitCounts.getSecondary()).isEqualTo(45L);
        assertThat(hitCounts.getDifference()).isEqualTo(5L);
    }

    @Test
    void hitCounts_ShouldCreateWithAllArgsConstructor() {
        // When
        DiffResult.HitCounts hitCounts = new DiffResult.HitCounts(30L, 30L, 0L);

        // Then
        assertThat(hitCounts.getPrimary()).isEqualTo(30L);
        assertThat(hitCounts.getSecondary()).isEqualTo(30L);
        assertThat(hitCounts.getDifference()).isEqualTo(0L);
    }

    @Test
    void contentDifference_ShouldCreateWithBuilder() {
        // When
        DiffResult.ContentDifference diff = DiffResult.ContentDifference.builder()
                .documentId("test-doc")
                .field("author")
                .primaryValue("John Doe")
                .secondaryValue("Jane Doe")
                .differenceType(DifferenceType.DIFFERENT)
                .build();

        // Then
        assertThat(diff.getDocumentId()).isEqualTo("test-doc");
        assertThat(diff.getField()).isEqualTo("author");
        assertThat(diff.getPrimaryValue()).isEqualTo("John Doe");
        assertThat(diff.getSecondaryValue()).isEqualTo("Jane Doe");
        assertThat(diff.getDifferenceType()).isEqualTo(DifferenceType.DIFFERENT);
    }

    @Test
    void contentDifference_ShouldHandleNullValues() {
        // When
        DiffResult.ContentDifference diff = DiffResult.ContentDifference.builder()
                .documentId("test-doc")
                .field("optional_field")
                .primaryValue(null)
                .secondaryValue("Some value")
                .differenceType(DifferenceType.MISSING_IN_PRIMARY)
                .build();

        // Then
        assertThat(diff.getPrimaryValue()).isNull();
        assertThat(diff.getSecondaryValue()).isEqualTo("Some value");
        assertThat(diff.getDifferenceType()).isEqualTo(DifferenceType.MISSING_IN_PRIMARY);
    }

    @Test
    void comparisonMetrics_ShouldCreateWithBuilder() {
        // Given
        Map<String, Integer> differencesByField = Map.of(
                "title", 2,
                "content", 3,
                "tags", 1
        );

        // When
        DiffResult.ComparisonMetrics metrics = DiffResult.ComparisonMetrics.builder()
                .totalDocumentsCompared(200L)
                .documentsWithDifferences(15L)
                .totalFieldDifferences(25L)
                .queryTimePrimary(300L)
                .queryTimeSecondary(280L)
                .comparisonTimeMs(45L)
                .differencesByField(differencesByField)
                .build();

        // Then
        assertThat(metrics.getTotalDocumentsCompared()).isEqualTo(200L);
        assertThat(metrics.getDocumentsWithDifferences()).isEqualTo(15L);
        assertThat(metrics.getTotalFieldDifferences()).isEqualTo(25L);
        assertThat(metrics.getQueryTimePrimary()).isEqualTo(300L);
        assertThat(metrics.getQueryTimeSecondary()).isEqualTo(280L);
        assertThat(metrics.getComparisonTimeMs()).isEqualTo(45L);
        assertThat(metrics.getDifferencesByField()).containsExactlyInAnyOrderEntriesOf(differencesByField);
    }

    @Test
    void jsonSerialization_ShouldIncludeNonNullFields() throws Exception {
        // Given
        DiffResult result = DiffResult.builder()
                .countsMatch(true)
                .summary("All counts match")
                .build();

        // When
        String json = objectMapper.writeValueAsString(result);

        // Then
        assertThat(json).contains("countsMatch");
        assertThat(json).contains("summary");
        assertThat(json).doesNotContain("hitCounts"); // Should be null and excluded
        assertThat(json).doesNotContain("onlyInPrimary"); // Should be null and excluded
    }

    @Test
    void jsonSerialization_ShouldExcludeNullFields() throws Exception {
        // Given
        DiffResult result = DiffResult.builder()
                .countsMatch(true)
                .summary("Test")
                .hitCounts(null)
                .onlyInPrimary(null)
                .onlyInSecondary(null)
                .contentDifferences(null)
                .metrics(null)
                .build();

        // When
        String json = objectMapper.writeValueAsString(result);

        // Then
        assertThat(json).doesNotContain("hitCounts");
        assertThat(json).doesNotContain("onlyInPrimary");
        assertThat(json).doesNotContain("onlyInSecondary");
        assertThat(json).doesNotContain("contentDifferences");
        assertThat(json).doesNotContain("metrics");
    }

    @Test
    void jsonDeserialization_ShouldCreateObjectFromJson() throws Exception {
        // Given
        String json = """
                {
                    "countsMatch": false,
                    "summary": "Differences found",
                    "onlyInPrimary": ["doc1"],
                    "onlyInSecondary": ["doc2"]
                }
                """;

        // When
        DiffResult result = objectMapper.readValue(json, DiffResult.class);

        // Then
        assertThat(result.isCountsMatch()).isFalse();
        assertThat(result.getSummary()).isEqualTo("Differences found");
        assertThat(result.getOnlyInPrimary()).containsExactly("doc1");
        assertThat(result.getOnlyInSecondary()).containsExactly("doc2");
        assertThat(result.getHitCounts()).isNull();
        assertThat(result.getContentDifferences()).isNull();
        assertThat(result.getMetrics()).isNull();
    }

    @Test
    void equals_ShouldWorkCorrectlyForHitCounts() {
        // Given
        DiffResult.HitCounts hitCounts1 = new DiffResult.HitCounts(10L, 8L, 2L);
        DiffResult.HitCounts hitCounts2 = new DiffResult.HitCounts(10L, 8L, 2L);
        DiffResult.HitCounts hitCounts3 = new DiffResult.HitCounts(10L, 9L, 1L);

        // Then
        assertThat(hitCounts1).isEqualTo(hitCounts2);
        assertThat(hitCounts1).isNotEqualTo(hitCounts3);
        assertThat(hitCounts1.hashCode()).isEqualTo(hitCounts2.hashCode());
    }

    @Test
    void equals_ShouldWorkCorrectlyForContentDifference() {
        // Given
        DiffResult.ContentDifference diff1 = new DiffResult.ContentDifference(
                "doc1", "title", "Old", "New", DifferenceType.DIFFERENT
        );
        DiffResult.ContentDifference diff2 = new DiffResult.ContentDifference(
                "doc1", "title", "Old", "New", DifferenceType.DIFFERENT
        );
        DiffResult.ContentDifference diff3 = new DiffResult.ContentDifference(
                "doc2", "title", "Old", "New", DifferenceType.DIFFERENT
        );

        // Then
        assertThat(diff1).isEqualTo(diff2);
        assertThat(diff1).isNotEqualTo(diff3);
        assertThat(diff1.hashCode()).isEqualTo(diff2.hashCode());
    }

    @Test
    void toString_ShouldProvideUsefulOutput() {
        // Given
        DiffResult.HitCounts hitCounts = new DiffResult.HitCounts(100L, 95L, 5L);

        // When
        String toString = hitCounts.toString();

        // Then
        assertThat(toString).contains("HitCounts");
        assertThat(toString).contains("primary=100");
        assertThat(toString).contains("secondary=95");
        assertThat(toString).contains("difference=5");
    }
}