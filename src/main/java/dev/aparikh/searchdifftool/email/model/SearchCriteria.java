package dev.aparikh.searchdifftool.email.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchCriteria {
    
    private String subject;
    private String from;
    private String to;
    private String body;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String attachmentName;
    private String query; // Raw query string
    
    @Builder.Default
    private int maxResults = 1000;
    
    @Builder.Default
    private int start = 0;
    
    // Additional filters
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();
    
    public Map<String, Object> toQueryMap() {
        Map<String, Object> queryMap = new HashMap<>();
        
        if (subject != null) queryMap.put("subject", subject);
        if (from != null) queryMap.put("from", from);
        if (to != null) queryMap.put("to", to);
        if (body != null) queryMap.put("body", body);
        if (dateFrom != null) queryMap.put("dateFrom", dateFrom);
        if (dateTo != null) queryMap.put("dateTo", dateTo);
        if (attachmentName != null) queryMap.put("attachments", attachmentName);
        
        // Add any additional filters
        queryMap.putAll(filters);
        
        return queryMap;
    }
}