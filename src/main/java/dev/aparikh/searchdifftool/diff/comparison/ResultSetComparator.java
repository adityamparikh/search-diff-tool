package dev.aparikh.searchdifftool.diff.comparison;

import dev.aparikh.searchdifftool.email.model.EmailDocument;
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
    
    public SetDifference findDifferences(List<EmailDocument> primaryDocs, 
                                       List<EmailDocument> secondaryDocs) {
        
        Set<String> primaryIds = primaryDocs.stream()
            .map(doc -> doc.id())
            .collect(Collectors.toSet());
            
        Set<String> secondaryIds = secondaryDocs.stream()
            .map(doc -> doc.id())
            .collect(Collectors.toSet());
        
        List<String> onlyInPrimary = primaryIds.stream()
            .filter(id -> !secondaryIds.contains(id))
            .collect(Collectors.toList());
            
        List<String> onlyInSecondary = secondaryIds.stream()
            .filter(id -> !primaryIds.contains(id))
            .collect(Collectors.toList());
        
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
        
        public SetDifference() {
            this.onlyInPrimary = new ArrayList<>();
            this.onlyInSecondary = new ArrayList<>();
        }
        
        public SetDifference(List<String> onlyInPrimary, List<String> onlyInSecondary) {
            this.onlyInPrimary = onlyInPrimary != null ? onlyInPrimary : new ArrayList<>();
            this.onlyInSecondary = onlyInSecondary != null ? onlyInSecondary : new ArrayList<>();
        }
    }
}