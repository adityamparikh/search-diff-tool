package dev.aparikh.searchdifftool.diff.comparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aparikh.searchdifftool.diff.DiffResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void constructor_WithValidParameters_ShouldCreateRecord() {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "title", "Old Title", "New Title")
        );
        Map<String, Integer> differencesByField = Map.of("title", 1, "content", 2);

        // When
        ComparisonResult result = new ComparisonResult(differences, 1L, 3L, differencesByField);

        // Then
        assertThat(result.differences()).isEqualTo(differences);
        assertThat(result.documentsWithDifferences()).isEqualTo(1L);
        assertThat(result.totalFieldDifferences()).isEqualTo(3L);
        assertThat(result.differencesByField()).isEqualTo(differencesByField);
    }

    @Test
    void constructor_WithNullDifferences_ShouldUseEmptyList() {
        // Given
        Map<String, Integer> differencesByField = Map.of("title", 1);

        // When
        ComparisonResult result = new ComparisonResult(null, 1L, 1L, differencesByField);

        // Then
        assertThat(result.differences()).isNotNull();
        assertThat(result.differences()).isEmpty();
        assertThat(result.documentsWithDifferences()).isEqualTo(1L);
        assertThat(result.totalFieldDifferences()).isEqualTo(1L);
        assertThat(result.differencesByField()).isEqualTo(differencesByField);
    }

    @Test
    void constructor_WithNullDifferencesByField_ShouldUseEmptyMap() {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "field1", "value1", "value2")
        );

        // When
        ComparisonResult result = new ComparisonResult(differences, 1L, 1L, null);

        // Then
        assertThat(result.differences()).isEqualTo(differences);
        assertThat(result.documentsWithDifferences()).isEqualTo(1L);
        assertThat(result.totalFieldDifferences()).isEqualTo(1L);
        assertThat(result.differencesByField()).isNotNull();
        assertThat(result.differencesByField()).isEmpty();
    }

    @Test
    void constructor_WithBothNullCollections_ShouldUseEmptyCollections() {
        // When
        ComparisonResult result = new ComparisonResult(null, 0L, 0L, null);

        // Then
        assertThat(result.differences()).isNotNull();
        assertThat(result.differences()).isEmpty();
        assertThat(result.documentsWithDifferences()).isEqualTo(0L);
        assertThat(result.totalFieldDifferences()).isEqualTo(0L);
        assertThat(result.differencesByField()).isNotNull();
        assertThat(result.differencesByField()).isEmpty();
    }

    @Test
    void constructor_WithEmptyCollections_ShouldPreserveEmptyCollections() {
        // Given
        List<DiffResult.ContentDifference> emptyDifferences = new ArrayList<>();
        Map<String, Integer> emptyMap = new HashMap<>();

        // When
        ComparisonResult result = new ComparisonResult(emptyDifferences, 0L, 0L, emptyMap);

        // Then
        assertThat(result.differences()).isSameAs(emptyDifferences);
        assertThat(result.differencesByField()).isSameAs(emptyMap);
        assertThat(result.documentsWithDifferences()).isEqualTo(0L);
        assertThat(result.totalFieldDifferences()).isEqualTo(0L);
    }

    @Test
    void record_ShouldSupportEquality() {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "title", "Old", "New")
        );
        Map<String, Integer> differencesByField = Map.of("title", 1);

        ComparisonResult result1 = new ComparisonResult(differences, 1L, 1L, differencesByField);
        ComparisonResult result2 = new ComparisonResult(differences, 1L, 1L, differencesByField);
        ComparisonResult result3 = new ComparisonResult(differences, 2L, 1L, differencesByField);

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void record_ShouldSupportToString() {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "title", "Old", "New")
        );
        Map<String, Integer> differencesByField = Map.of("title", 1);

        ComparisonResult result = new ComparisonResult(differences, 1L, 1L, differencesByField);

        // When
        String toString = result.toString();

        // Then
        assertThat(toString).contains("ComparisonResult");
        assertThat(toString).contains("differences=");
        assertThat(toString).contains("documentsWithDifferences=1");
        assertThat(toString).contains("totalFieldDifferences=1");
        assertThat(toString).contains("differencesByField=");
    }

    @Test
    void jsonSerialization_ShouldSerializeCorrectly() throws Exception {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "title", "Old Title", "New Title")
        );
        Map<String, Integer> differencesByField = Map.of("title", 1, "content", 2);

        ComparisonResult result = new ComparisonResult(differences, 1L, 3L, differencesByField);

        // When
        String json = objectMapper.writeValueAsString(result);

        // Then
        assertThat(json).contains("\"documentsWithDifferences\":1");
        assertThat(json).contains("\"totalFieldDifferences\":3");
        assertThat(json).contains("\"differences\":");
        assertThat(json).contains("\"differencesByField\":");
    }

    @Test
    void jsonDeserialization_ShouldDeserializeCorrectly() throws Exception {
        // Given
        String json = """
                {
                    "differences": [],
                    "documentsWithDifferences": 5,
                    "totalFieldDifferences": 10,
                    "differencesByField": {
                        "title": 3,
                        "content": 7
                    }
                }
                """;

        // When
        ComparisonResult result = objectMapper.readValue(json, ComparisonResult.class);

        // Then
        assertThat(result.differences()).isEmpty();
        assertThat(result.documentsWithDifferences()).isEqualTo(5L);
        assertThat(result.totalFieldDifferences()).isEqualTo(10L);
        assertThat(result.differencesByField()).containsEntry("title", 3);
        assertThat(result.differencesByField()).containsEntry("content", 7);
    }

    @Test
    void jsonRoundTrip_ShouldPreserveData() throws Exception {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "title", "Old", "New"),
                createContentDifference("doc2", "content", "Original", "Modified")
        );
        Map<String, Integer> differencesByField = Map.of("title", 1, "content", 1);

        ComparisonResult original = new ComparisonResult(differences, 2L, 2L, differencesByField);

        // When
        String json = objectMapper.writeValueAsString(original);
        ComparisonResult deserialized = objectMapper.readValue(json, ComparisonResult.class);

        // Then
        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.differences()).hasSize(2);
        assertThat(deserialized.documentsWithDifferences()).isEqualTo(2L);
        assertThat(deserialized.totalFieldDifferences()).isEqualTo(2L);
        assertThat(deserialized.differencesByField()).containsExactlyInAnyOrderEntriesOf(differencesByField);
    }

    @Test
    void record_WithComplexData_ShouldHandleCorrectly() {
        // Given
        List<DiffResult.ContentDifference> differences = List.of(
                createContentDifference("doc1", "title", "Title 1", "Modified Title 1"),
                createContentDifference("doc1", "content", "Content 1", "Modified Content 1"),
                createContentDifference("doc2", "title", null, "New Title"),
                createContentDifference("doc3", "tags", List.of("tag1", "tag2"), List.of("tag1", "tag3"))
        );
        
        Map<String, Integer> differencesByField = Map.of(
                "title", 2,
                "content", 1, 
                "tags", 1
        );

        // When
        ComparisonResult result = new ComparisonResult(differences, 3L, 4L, differencesByField);

        // Then
        assertThat(result.differences()).hasSize(4);
        assertThat(result.documentsWithDifferences()).isEqualTo(3L);
        assertThat(result.totalFieldDifferences()).isEqualTo(4L);
        assertThat(result.differencesByField()).hasSize(3);
        assertThat(result.differencesByField().get("title")).isEqualTo(2);
        assertThat(result.differencesByField().get("content")).isEqualTo(1);
        assertThat(result.differencesByField().get("tags")).isEqualTo(1);
    }

    private DiffResult.ContentDifference createContentDifference(
            String documentId, String field, Object primaryValue, Object secondaryValue) {
        return DiffResult.ContentDifference.builder()
                .documentId(documentId)
                .field(field)
                .primaryValue(primaryValue)
                .secondaryValue(secondaryValue)
                .differenceType(DifferenceType.DIFFERENT)
                .build();
    }
}