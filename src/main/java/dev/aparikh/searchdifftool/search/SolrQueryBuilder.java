package dev.aparikh.searchdifftool.search;

import dev.aparikh.searchdifftool.normalization.FieldMappingConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolrQueryBuilder {
    
    private final FieldMappingConfigurationProperties fieldMappingConfigurationProperties;
    
    public SolrQuery buildQueryForPrimary(SearchCriteria criteria) {
        Map<String, String> mapping = fieldMappingConfigurationProperties.getPrimaryToCommon();
        
        return buildSolrQuery(criteria, getReverseMapping(mapping));
    }
    
    public SolrQuery buildQueryForSecondary(SearchCriteria criteria) {
        Map<String, String> mapping = fieldMappingConfigurationProperties.getSecondaryToCommon();
        
        return buildSolrQuery(criteria, getReverseMapping(mapping));
    }
    
    private SolrQuery buildSolrQuery(SearchCriteria criteria,
                                     Map<String, String> mapping) {
        SolrQuery query = new SolrQuery();
        
        // Handle raw query if provided
        if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            query.setQuery(criteria.getQuery());
        } else {
            // Build structured query from criteria
            Map<String, Object> queryMap = criteria.toQueryMap();
            StringBuilder queryBuilder = new StringBuilder();
            
            for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
                String commonField = entry.getKey();
                String solrField = mapping.getOrDefault(commonField, commonField);
                Object value = entry.getValue();
                
                if (!queryBuilder.isEmpty()) {
                    queryBuilder.append(" AND ");
                }
                
                if (value instanceof String val && val.contains(" ")) {
                    queryBuilder.append(solrField).append(":\"").append(value).append("\"");
                } else {
                    queryBuilder.append(solrField).append(":").append(value);
                }
            }
            
            query.setQuery(!queryBuilder.isEmpty() ? queryBuilder.toString() : "*:*");
        }
        
        // Set pagination parameters
        query.setRows(criteria.getMaxResults());
        query.setStart(criteria.getStart());
        
        return query;
    }

    
    private Map<String, String> getReverseMapping(Map<String, String> mapping) {
        Map<String, String> reverse = new HashMap<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return reverse;
    }
}