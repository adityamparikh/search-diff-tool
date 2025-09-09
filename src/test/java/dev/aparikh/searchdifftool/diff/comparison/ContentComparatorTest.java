package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.DiffResult;
import dev.aparikh.searchdifftool.diff.comparison.DifferenceType;
import dev.aparikh.searchdifftool.normalization.FieldMappingConfigurationProperties;
import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentComparatorTest {

    @Mock
    private FieldMappingConfigurationProperties fieldMappingConfig;

    private ContentComparator contentComparator;
    private Map<String, FieldMappingConfigurationProperties.FieldMapping> defaultFieldMappings;

    @BeforeEach
    void setUp() {
        defaultFieldMappings = new HashMap<>();
        defaultFieldMappings.put("subject", new FieldMappingConfigurationProperties.FieldMapping("subject", "subject", "string"));
        defaultFieldMappings.put("body", new FieldMappingConfigurationProperties.FieldMapping("body", "body", "string"));
        defaultFieldMappings.put("from", new FieldMappingConfigurationProperties.FieldMapping("from", "from", "string"));

        lenient().when(fieldMappingConfig.getFields()).thenReturn(defaultFieldMappings);
        contentComparator = new ContentComparator(fieldMappingConfig);
    }

    @Test
    void compareDocuments_WithIdenticalDocuments_ReturnsNoDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Collections.singletonList(
            createDocument("1", "Test Subject", "Test Content", "Author")
        );
        List<NormalizedDocument> secondaryDocs = Collections.singletonList(
            createDocument("1", "Test Subject", "Test Content", "Author")
        );

        // When
        ComparisonResult result = contentComparator.compareDocuments(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.differences()).isEmpty();
        assertThat(result.documentsWithDifferences()).isZero();
        assertThat(result.totalFieldDifferences()).isZero();
        assertThat(result.differencesByField()).isEmpty();
    }

    @Test
    void compareDocuments_WithDifferentFieldValues_ReturnsFieldDifferences() {
        // Given
        List<NormalizedDocument> primaryDocs = Collections.singletonList(
            createDocument("1", "Old Subject", "Old Content", "Old Author")
        );
        List<NormalizedDocument> secondaryDocs = Collections.singletonList(
            createDocument("1", "New Subject", "New Content", "New Author")
        );

        // When
        ComparisonResult result = contentComparator.compareDocuments(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.differences()).hasSize(3);
        assertThat(result.documentsWithDifferences()).isEqualTo(1);
        assertThat(result.totalFieldDifferences()).isEqualTo(3);
        assertThat(result.differencesByField())
            .containsEntry("subject", 1)
            .containsEntry("body", 1)
            .containsEntry("from", 1);
    }

    @Test
    void compareDocuments_WithNullValues_HandlesDifferencesCorrectly() {
        // Given
        List<NormalizedDocument> primaryDocs = Collections.singletonList(
            createDocument("1", "Subject", null, "Author")
        );
        List<NormalizedDocument> secondaryDocs = Collections.singletonList(
            createDocument("1", "Subject", "New Content", null)
        );

        // When
        ComparisonResult result = contentComparator.compareDocuments(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.differences()).hasSize(2);
        assertThat(result.documentsWithDifferences()).isEqualTo(1);
        assertThat(result.totalFieldDifferences()).isEqualTo(2);

        // Verify specific differences
        List<DiffResult.ContentDifference> diffs = result.differences();
        assertThat(diffs).anySatisfy(diff -> {
            assertThat(diff.getField()).isEqualTo("body");
            assertThat(diff.getPrimaryValue()).isNull();
            assertThat(diff.getSecondaryValue()).isEqualTo("New Content");
            assertThat(diff.getDifferenceType()).isEqualTo(DifferenceType.MISSING_IN_PRIMARY);
        });
        assertThat(diffs).anySatisfy(diff -> {
            assertThat(diff.getField()).isEqualTo("from");
            assertThat(diff.getPrimaryValue()).isEqualTo("Author");
            assertThat(diff.getSecondaryValue()).isNull();
            assertThat(diff.getDifferenceType()).isEqualTo(DifferenceType.MISSING_IN_SECONDARY);
        });
    }

    @Test
    void compareDocuments_WithMetadataDifferences_DetectsMetadataChanges() {
        // Given
        Map<String, Object> primaryMetadata = new HashMap<>();
        primaryMetadata.put("key1", "value1");
        primaryMetadata.put("key2", "value2");

        Map<String, Object> secondaryMetadata = new HashMap<>();
        secondaryMetadata.put("key1", "value1");
        secondaryMetadata.put("key2", "different");
        secondaryMetadata.put("key3", "value3");

        List<NormalizedDocument> primaryDocs = Collections.singletonList(
            createDocumentWithMetadata("1", "Subject", "Content", "Author", primaryMetadata)
        );
        List<NormalizedDocument> secondaryDocs = Collections.singletonList(
            createDocumentWithMetadata("1", "Subject", "Content", "Author", secondaryMetadata)
        );

        // When
        ComparisonResult result = contentComparator.compareDocuments(primaryDocs, secondaryDocs);

        // Then
        assertThat(result.differences()).hasSize(2);
        assertThat(result.documentsWithDifferences()).isEqualTo(1);
        assertThat(result.totalFieldDifferences()).isEqualTo(2);

        // Verify metadata differences
        List<DiffResult.ContentDifference> diffs = result.differences();
        assertThat(diffs).anySatisfy(diff -> {
            assertThat(diff.getField()).isEqualTo("metadata.key2");
            assertThat(diff.getPrimaryValue()).isEqualTo("value2");
            assertThat(diff.getSecondaryValue()).isEqualTo("different");
            assertThat(diff.getDifferenceType()).isEqualTo(DifferenceType.DIFFERENT);
        });
        assertThat(diffs).anySatisfy(diff -> {
            assertThat(diff.getField()).isEqualTo("metadata.key3");
            assertThat(diff.getPrimaryValue()).isNull();
            assertThat(diff.getSecondaryValue()).isEqualTo("value3");
            assertThat(diff.getDifferenceType()).isEqualTo(DifferenceType.MISSING_IN_PRIMARY);
        });
    }

    @Test
    void compareDocuments_WithEmptyDocuments_ReturnsEmptyResult() {
        // When
        ComparisonResult result = contentComparator.compareDocuments(
            Collections.emptyList(),
            Collections.emptyList()
        );

        // Then
        assertThat(result.differences()).isEmpty();
        assertThat(result.documentsWithDifferences()).isZero();
        assertThat(result.totalFieldDifferences()).isZero();
        assertThat(result.differencesByField()).isEmpty();
    }

    private NormalizedDocument createDocument(String id, String subject, String body, String from) {
        return createDocumentWithMetadata(id, subject, body, from, Collections.emptyMap());
    }

    private NormalizedDocument createDocumentWithMetadata(
            String id, String subject, String body, String from, Map<String, Object> metadata) {
        return NormalizedDocument.builder()
            .id(id)
            .subject(subject)
            .body(body)
            .from(from)
            .to("recipient@example.com")
            .date(LocalDateTime.now())
            .attachments(Collections.emptyList())
            .metadata(metadata)
            .source("test")
            .build();
    }
}
