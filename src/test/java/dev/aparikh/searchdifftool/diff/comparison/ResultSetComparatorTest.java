package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultSetComparatorTest {

    private ResultSetComparator resultSetComparator;

    @BeforeEach
    void setUp() {
        resultSetComparator = new ResultSetComparator();
    }

    @Test
    void findDifferences_WithIdenticalSets_ReturnsEmptyDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2")
        );
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2")
        );

        // When
        ResultSetComparator.SetDifference result = resultSetComparator.findDifferences(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.getOnlyInPrimary()).isEmpty();
        assertThat(result.getOnlyInSecondary()).isEmpty();
    }

    @Test
    void findDifferences_WithDocumentsOnlyInPrimary_ReturnsCorrectDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2"),
            createDocument("3", "Doc 3")
        );
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2")
        );

        // When
        ResultSetComparator.SetDifference result = resultSetComparator.findDifferences(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.getOnlyInPrimary()).hasSize(1);
        assertThat(result.getOnlyInPrimary().get(0)).isEqualTo("3");
        assertThat(result.getOnlyInSecondary()).isEmpty();
    }

    @Test
    void findDifferences_WithDocumentsOnlyInSecondary_ReturnsCorrectDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2")
        );
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2"),
            createDocument("3", "Doc 3")
        );

        // When
        ResultSetComparator.SetDifference result = resultSetComparator.findDifferences(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.getOnlyInPrimary()).isEmpty();
        assertThat(result.getOnlyInSecondary()).hasSize(1);
        assertThat(result.getOnlyInSecondary().get(0)).isEqualTo("3");
    }

    @Test
    void findDifferences_WithDifferentDocumentsInBothSets_ReturnsAllDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2"),
            createDocument("3", "Doc 3")
        );
        List<NormalizedDocument> secondaryDocs = Arrays.asList(
            createDocument("1", "Doc 1"),
            createDocument("2", "Doc 2"),
            createDocument("4", "Doc 4")
        );

        // When
        ResultSetComparator.SetDifference result = resultSetComparator.findDifferences(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.getOnlyInPrimary()).hasSize(1);
        assertThat(result.getOnlyInPrimary().get(0)).isEqualTo("3");
        assertThat(result.getOnlyInSecondary()).hasSize(1);
        assertThat(result.getOnlyInSecondary().get(0)).isEqualTo("4");
    }

    @Test
    void findDifferences_WithEmptySets_ReturnsEmptyDifferences() {
        // Given
        List<NormalizedDocument> emptyList = Collections.emptyList();

        // When
        ResultSetComparator.SetDifference result = resultSetComparator.findDifferences(emptyList, emptyList);

        // Then
        assertThat(result.getOnlyInPrimary()).isEmpty();
        assertThat(result.getOnlyInSecondary()).isEmpty();
    }

    @Test
    void findDifferences_WithNullSets_HandlesGracefully() {
        // Given
        List<NormalizedDocument> docs = Arrays.asList(createDocument("1", "Doc 1"));

        // When
        ResultSetComparator.SetDifference resultWithNullPrimary = resultSetComparator.findDifferences(null, docs);
        ResultSetComparator.SetDifference resultWithNullSecondary = resultSetComparator.findDifferences(docs, null);

        // Then
        assertThat(resultWithNullPrimary.getOnlyInPrimary()).isEmpty();
        assertThat(resultWithNullPrimary.getOnlyInSecondary()).hasSize(1);

        assertThat(resultWithNullSecondary.getOnlyInPrimary()).hasSize(1);
        assertThat(resultWithNullSecondary.getOnlyInSecondary()).isEmpty();
    }

    private NormalizedDocument createDocument(String id, String subject) {
        return NormalizedDocument.builder()
            .id(id)
            .subject(subject)
            .from("test@example.com")
            .to("recipient@example.com")
            .body("Test body")
            .date(LocalDateTime.now())
            .attachments(Collections.emptyList())
            .metadata(Collections.emptyMap())
            .source("test")
            .build();
    }
}
