package dev.aparikh.searchdifftool.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRequest {
    
    private String subject;
    private String from;
    private String to;
    private String body;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String attachmentName;
    private String query;
    
    @Min(1)
    @Max(10000)
    @Builder.Default
    private Integer maxResults = 1000;
    
    @Min(0)
    @Builder.Default
    private Integer start = 0;
    
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();
}