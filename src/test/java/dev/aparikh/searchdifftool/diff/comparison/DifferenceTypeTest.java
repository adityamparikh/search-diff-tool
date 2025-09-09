package dev.aparikh.searchdifftool.diff.comparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifferenceTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void allEnumValues_ShouldBePresent() {
        // When
        DifferenceType[] values = DifferenceType.values();

        // Then
        assertThat(values).hasSize(4);
        assertThat(values).containsExactlyInAnyOrder(
                DifferenceType.MISSING_IN_PRIMARY,
                DifferenceType.MISSING_IN_SECONDARY,
                DifferenceType.TYPE_MISMATCH,
                DifferenceType.DIFFERENT
        );
    }

    @Test
    void valueOf_ShouldReturnCorrectEnum() {
        // When & Then
        assertThat(DifferenceType.valueOf("MISSING_IN_PRIMARY")).isEqualTo(DifferenceType.MISSING_IN_PRIMARY);
        assertThat(DifferenceType.valueOf("MISSING_IN_SECONDARY")).isEqualTo(DifferenceType.MISSING_IN_SECONDARY);
        assertThat(DifferenceType.valueOf("TYPE_MISMATCH")).isEqualTo(DifferenceType.TYPE_MISMATCH);
        assertThat(DifferenceType.valueOf("DIFFERENT")).isEqualTo(DifferenceType.DIFFERENT);
    }

    @Test
    void enumName_ShouldReturnCorrectString() {
        // When & Then
        assertThat(DifferenceType.MISSING_IN_PRIMARY.name()).isEqualTo("MISSING_IN_PRIMARY");
        assertThat(DifferenceType.MISSING_IN_SECONDARY.name()).isEqualTo("MISSING_IN_SECONDARY");
        assertThat(DifferenceType.TYPE_MISMATCH.name()).isEqualTo("TYPE_MISMATCH");
        assertThat(DifferenceType.DIFFERENT.name()).isEqualTo("DIFFERENT");
    }

    @Test
    void enumOrdinal_ShouldReturnConsistentValues() {
        // When & Then
        assertThat(DifferenceType.MISSING_IN_PRIMARY.ordinal()).isEqualTo(0);
        assertThat(DifferenceType.MISSING_IN_SECONDARY.ordinal()).isEqualTo(1);
        assertThat(DifferenceType.TYPE_MISMATCH.ordinal()).isEqualTo(2);
        assertThat(DifferenceType.DIFFERENT.ordinal()).isEqualTo(3);
    }

    @Test
    void jsonSerialization_ShouldSerializeAsString() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(DifferenceType.MISSING_IN_PRIMARY);

        // Then
        assertThat(json).isEqualTo("\"MISSING_IN_PRIMARY\"");
    }

    @Test
    void jsonDeserialization_ShouldDeserializeFromString() throws Exception {
        // Given
        String json = "\"TYPE_MISMATCH\"";

        // When
        DifferenceType result = objectMapper.readValue(json, DifferenceType.class);

        // Then
        assertThat(result).isEqualTo(DifferenceType.TYPE_MISMATCH);
    }

    @Test
    void jsonSerialization_AllValues_ShouldRoundTrip() throws Exception {
        // Given
        DifferenceType[] allTypes = DifferenceType.values();

        for (DifferenceType type : allTypes) {
            // When
            String json = objectMapper.writeValueAsString(type);
            DifferenceType deserialized = objectMapper.readValue(json, DifferenceType.class);

            // Then
            assertThat(deserialized).isEqualTo(type);
        }
    }

    @Test
    void toString_ShouldReturnEnumName() {
        // When & Then
        assertThat(DifferenceType.MISSING_IN_PRIMARY.toString()).isEqualTo("MISSING_IN_PRIMARY");
        assertThat(DifferenceType.MISSING_IN_SECONDARY.toString()).isEqualTo("MISSING_IN_SECONDARY");
        assertThat(DifferenceType.TYPE_MISMATCH.toString()).isEqualTo("TYPE_MISMATCH");
        assertThat(DifferenceType.DIFFERENT.toString()).isEqualTo("DIFFERENT");
    }

    @Test
    void equals_ShouldWorkCorrectly() {
        // Given
        DifferenceType type1 = DifferenceType.DIFFERENT;
        DifferenceType type2 = DifferenceType.DIFFERENT;
        DifferenceType type3 = DifferenceType.TYPE_MISMATCH;

        // Then
        assertThat(type1).isEqualTo(type2);
        assertThat(type1).isNotEqualTo(type3);
        assertThat(type1.hashCode()).isEqualTo(type2.hashCode());
    }

    @Test
    void compareTo_ShouldWorkBasedOnOrdinal() {
        // When & Then
        assertThat(DifferenceType.MISSING_IN_PRIMARY.compareTo(DifferenceType.MISSING_IN_SECONDARY)).isLessThan(0);
        assertThat(DifferenceType.TYPE_MISMATCH.compareTo(DifferenceType.DIFFERENT)).isLessThan(0);
        assertThat(DifferenceType.DIFFERENT.compareTo(DifferenceType.MISSING_IN_PRIMARY)).isGreaterThan(0);
        assertThat(DifferenceType.TYPE_MISMATCH.compareTo(DifferenceType.TYPE_MISMATCH)).isEqualTo(0);
    }
}