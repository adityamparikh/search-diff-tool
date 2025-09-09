package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.DiffResult;
import dev.aparikh.searchdifftool.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class HitCountComparatorTest {

    private HitCountComparator hitCountComparator;

    @BeforeEach
    void setUp() {
        hitCountComparator = new HitCountComparator();
    }

    @Test
    void compare_WhenHitCountsMatch_ReturnsZeroDifference() {
        // Given
        SearchResult primaryResult = new SearchResult(Collections.emptyList(), 10L, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 10L, "secondary", 100L);

        // When
        DiffResult.HitCounts result = hitCountComparator.compare(primaryResult, secondaryResult);

        // Then
        assertThat(result.getPrimary()).isEqualTo(10);
        assertThat(result.getSecondary()).isEqualTo(10);
        assertThat(result.getDifference()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "15, 10, 5",
        "10, 15, -5",
        "0, 10, -10",
        "10, 0, 10"
    })
    void compare_WhenHitCountsDiffer_ReturnsCorrectDifference(int primary, int secondary, int expectedDiff) {
        // Given
        SearchResult primaryResult = new SearchResult(Collections.emptyList(), (long) primary, "primary", 100L);
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), (long) secondary, "secondary", 100L);

        // When
        DiffResult.HitCounts result = hitCountComparator.compare(primaryResult, secondaryResult);

        // Then
        assertThat(result.getPrimary()).isEqualTo(primary);
        assertThat(result.getSecondary()).isEqualTo(secondary);
        assertThat(result.getDifference()).isEqualTo(expectedDiff);
    }

    @Test
    void compare_WithNullResults_HandlesGracefully() {
        // Given
        SearchResult primaryResult = null;
        SearchResult secondaryResult = new SearchResult(Collections.emptyList(), 10L, "secondary", 100L);

        // When
        DiffResult.HitCounts result = hitCountComparator.compare(primaryResult, secondaryResult);

        // Then
        assertThat(result.getPrimary()).isZero();
        assertThat(result.getSecondary()).isEqualTo(10);
        assertThat(result.getDifference()).isEqualTo(-10);
    }
}
