package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.diff.DiffResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Record representing the result of a content comparison operation.
 * Contains differences found, metrics, and summary information.
 */
public record ComparisonResult(
    List<DiffResult.ContentDifference> differences,
    long documentsWithDifferences,
    long totalFieldDifferences,
    Map<String, Integer> differencesByField
) {
    
    /**
     * Creates a ComparisonResult with null-safe defaults.
     */
    public ComparisonResult {
        differences = differences != null ? differences : new ArrayList<>();
        differencesByField = differencesByField != null ? differencesByField : new HashMap<>();
    }

}