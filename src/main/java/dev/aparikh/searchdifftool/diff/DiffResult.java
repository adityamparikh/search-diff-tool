package dev.aparikh.searchdifftool.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.aparikh.searchdifftool.diff.comparison.DifferenceType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiffResult {
    
    private HitCounts hitCounts;
    private boolean countsMatch;
    private List<String> onlyInPrimary;
    private List<String> onlyInSecondary;
    private List<ContentDifference> contentDifferences;
    private String summary;
    private ComparisonMetrics metrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HitCounts {
        private long primary;
        private long secondary;
        private long difference;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentDifference {
        private String documentId;
        private String field;
        private Object primaryValue;
        private Object secondaryValue;
        private DifferenceType differenceType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonMetrics {
        private long totalDocumentsCompared;
        private long documentsWithDifferences;
        private long totalFieldDifferences;
        private long queryTimePrimary;
        private long queryTimeSecondary;
        private long comparisonTimeMs;
        private Map<String, Integer> differencesByField;
    }
}