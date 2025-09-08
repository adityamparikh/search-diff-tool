package dev.aparikh.searchdifftool.search.solr.schema;

import dev.aparikh.searchdifftool.normalization.FieldMappingConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaMapper {
    
    private final FieldMappingConfigurationProperties fieldMappingConfigurationProperties;
    
    public SolrQuery mapQueryForPrimary(Map<String, Object> searchCriteria) {
        SolrQuery query = new SolrQuery();
        Map<String, String> mapping = fieldMappingConfigurationProperties.getPrimaryToCommon();
        
        return buildSolrQuery(query, searchCriteria, getReverseMapping(mapping));
    }
    
    public SolrQuery mapQueryForSecondary(Map<String, Object> searchCriteria) {
        SolrQuery query = new SolrQuery();
        Map<String, String> mapping = fieldMappingConfigurationProperties.getSecondaryToCommon();
        
        return buildSolrQuery(query, searchCriteria, getReverseMapping(mapping));
    }
    
    private SolrQuery buildSolrQuery(SolrQuery query, Map<String, Object> criteria, Map<String, String> mapping) {
        StringBuilder queryBuilder = new StringBuilder();
        
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
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
        return query;
    }
    
    public Map<String, Object> mapDocumentToCommon(SolrDocument doc, boolean isPrimary) {
        Map<String, String> mapping = isPrimary ? 
            fieldMappingConfigurationProperties.getPrimaryToCommon() :
            fieldMappingConfigurationProperties.getSecondaryToCommon();
            
        Map<String, Object> commonDoc = new HashMap<>();
        
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String solrField = entry.getKey();
            String commonField = entry.getValue();
            
            if (doc.containsKey(solrField)) {
                commonDoc.put(commonField, doc.getFieldValue(solrField));
            }
        }
        
        // Also include any unmapped fields
        for (String fieldName : doc.getFieldNames()) {
            if (!mapping.containsKey(fieldName)) {
                commonDoc.put(fieldName, doc.getFieldValue(fieldName));
            }
        }
        
        return commonDoc;
    }
    
    private Map<String, String> getReverseMapping(Map<String, String> mapping) {
        Map<String, String> reverse = new HashMap<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return reverse;
    }
}