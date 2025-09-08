package dev.aparikh.searchdifftool.email.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailDocument {
    
    private String id;
    private String subject;
    private String from;
    private String to;
    private String body;
    private LocalDateTime date;
    private List<String> attachments;
    private Map<String, Object> metadata;
    
    // For comparison purposes
    private String source; // "primary" or "secondary"
}