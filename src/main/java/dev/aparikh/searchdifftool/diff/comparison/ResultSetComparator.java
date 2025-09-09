package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.normalization.NormalizedDocument;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ResultSetComparator {
    
    public SetDifference findDifferences(List<NormalizedDocument> primaryDocs,
                                       List<NormalizedDocument> secondaryDocs) {
        
        Set<String> primaryIds = primaryDocs != null ? primaryDocs.stream()
            .map(NormalizedDocument::getId)
            .collect(Collectors.toSet()) : Set.of();
            
        Set<String> secondaryIds = secondaryDocs != null ? secondaryDocs.stream()
            .map(NormalizedDocument::getId)
            .collect(Collectors.toSet()) : Set.of();
        
        List<String> onlyInPrimary = primaryIds.stream()
            .filter(id -> !secondaryIds.contains(id))
            .toList();

        List<String> onlyInSecondary = secondaryIds.stream()
            .filter(id -> !primaryIds.contains(id))
            .toList();
        
        log.info("Set differences - Only in primary: {}, Only in secondary: {}", 
            onlyInPrimary.size(), onlyInSecondary.size());
        
        if (!onlyInPrimary.isEmpty()) {
            log.debug("Documents only in primary: {}", onlyInPrimary);
        }
        
        if (!onlyInSecondary.isEmpty()) {
            log.debug("Documents only in secondary: {}", onlyInSecondary);
        }
        
        return SetDifference.builder()
            .onlyInPrimary(onlyInPrimary)
            .onlyInSecondary(onlyInSecondary)
            .build();
    }
    
    @Data
    @lombok.Builder
    public static class SetDifference {
        private List<String> onlyInPrimary;
        private List<String> onlyInSecondary;
    }
}