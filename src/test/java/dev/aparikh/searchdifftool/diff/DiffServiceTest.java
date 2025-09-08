package dev.aparikh.searchdifftool.diff;

import dev.aparikh.searchdifftool.diff.comparison.ContentComparator;
import dev.aparikh.searchdifftool.diff.comparison.HitCountComparator;
import dev.aparikh.searchdifftool.diff.comparison.ResultSetComparator;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
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
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(Collections.emptyList())
                        .documentsWithDifferences(0L)
                        .totalFieldDifferences(0L)
                        .differencesByField(Collections.emptyMap())
                        .build());

        // When
        DiffService.DiffAnalysis result = diffService.compareDocs(primaryDocs, secondaryDocs);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSetDifference().getOnlyInPrimary()).isEmpty();
        assertThat(result.getSetDifference().getOnlyInSecondary()).isEmpty();
        assertThat(result.getContentResult().getDifferences()).isEmpty();
        assertThat(result.getContentResult().getTotalFieldDifferences()).isEqualTo(0L);
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
                .thenReturn(ContentComparator.ComparisonResult.builder()
                        .differences(Arrays.asList())
                        .documentsWithDifferences(1L)
                        .totalFieldDifferences(1L)
                        .differencesByField(Map.of("subject", 1))
                        .build());

        // When
        DiffService.DiffAnalysis result = diffService.compareDocs(primaryDocs, secondaryDocs);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSetDifference().getOnlyInPrimary()).hasSize(1);
        assertThat(result.getSetDifference().getOnlyInSecondary()).hasSize(1);
        assertThat(result.getContentResult().getDocumentsWithDifferences()).isEqualTo(1L);
        assertThat(result.getContentResult().getTotalFieldDifferences()).isEqualTo(1L);
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
}