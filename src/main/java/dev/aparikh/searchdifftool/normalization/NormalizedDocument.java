package dev.aparikh.searchdifftool.normalization;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedDocument {
    private String id;
    private String subject;
    private String from;
    private String to;
    private String body;
    private LocalDateTime date;
    private List<String> attachments;
    private Map<String, Object> metadata;
    private String source; // For comparison purposes - "primary" or "secondary"
}