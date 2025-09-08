package dev.aparikh.searchdifftool.email;

import dev.aparikh.searchdifftool.email.model.EmailDocument;
import dev.aparikh.searchdifftool.email.model.SearchCriteria;
import dev.aparikh.searchdifftool.solr.client.SolrClientService;
import dev.aparikh.searchdifftool.solr.schema.FieldMappingConfig;
import dev.aparikh.searchdifftool.solr.schema.SchemaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSearchService {
    
    private final SolrClientService solrClientService;
    private final SchemaMapper schemaMapper;
    private final FieldMappingConfig fieldMappingConfig;
    
    public SearchResult searchPrimary(SearchCriteria criteria) {
        try {
            SolrQuery query = schemaMapper.mapQueryForPrimary(criteria.toQueryMap());
            query.setRows(criteria.getMaxResults());
            query.setStart(criteria.getStart());
            
            QueryResponse response = solrClientService.getPrimaryClient().query(query);
            return convertToSearchResult(response, true);
            
        } catch (SolrServerException | IOException e) {
            log.error("Error searching primary Solr instance", e);
            throw new RuntimeException("Primary search failed", e);
        }
    }
    
    public SearchResult searchSecondary(SearchCriteria criteria) {
        try {
            SolrQuery query = schemaMapper.mapQueryForSecondary(criteria.toQueryMap());
            query.setRows(criteria.getMaxResults());
            query.setStart(criteria.getStart());
            
            QueryResponse response = solrClientService.getSecondaryClient().query(query);
            return convertToSearchResult(response, false);
            
        } catch (SolrServerException | IOException e) {
            log.error("Error searching secondary Solr instance", e);
            throw new RuntimeException("Secondary search failed", e);
        }
    }
    
    private SearchResult convertToSearchResult(QueryResponse response, boolean isPrimary) {
        SolrDocumentList docs = response.getResults();
        List<EmailDocument> emailDocs = new ArrayList<>();
        
        for (SolrDocument doc : docs) {
            EmailDocument emailDoc = convertToEmailDocument(doc, isPrimary);
            emailDocs.add(emailDoc);
        }
        
        return SearchResult.builder()
            .documents(emailDocs)
            .totalHits(docs.getNumFound())
            .source(isPrimary ? "primary" : "secondary")
            .queryTime(response.getQTime())
            .build();
    }
    
    private EmailDocument convertToEmailDocument(SolrDocument doc, boolean isPrimary) {
        Map<String, Object> commonDoc = schemaMapper.mapDocumentToCommon(doc, isPrimary);
        
        // Build field values array dynamically based on configuration
        Object[] fieldValues = new Object[9]; // EmailDocument has 9 fields
        
        // Get field values dynamically using configuration
        for (Map.Entry<String, FieldMappingConfig.FieldMapping> entry : fieldMappingConfig.getFields().entrySet()) {
            String commonFieldName = entry.getKey();
            String dataType = entry.getValue().type();
            
            Object value = getValueByType(commonDoc, commonFieldName, dataType);
            setFieldValueByName(fieldValues, commonFieldName, value);
        }
        
        // Set metadata and source
        fieldValues[7] = commonDoc; // metadata
        fieldValues[8] = isPrimary ? "primary" : "secondary"; // source
        
        return new EmailDocument(
            (String) fieldValues[0], // id
            (String) fieldValues[1], // subject  
            (String) fieldValues[2], // from
            (String) fieldValues[3], // to
            (String) fieldValues[4], // body
            (LocalDateTime) fieldValues[5], // date
            (List<String>) fieldValues[6], // attachments
            (Map<String, Object>) fieldValues[7], // metadata
            (String) fieldValues[8] // source
        );
    }
    
    private Object getValueByType(Map<String, Object> doc, String fieldName, String dataType) {
        return switch (dataType.toLowerCase()) {
            case "string" -> getStringValue(doc, fieldName);
            case "date" -> getDateValue(doc, fieldName);
            case "list" -> getListValue(doc, fieldName);
            default -> {
                log.warn("Unknown data type '{}' for field '{}', treating as string", dataType, fieldName);
                yield getStringValue(doc, fieldName);
            }
        };
    }
    
    private void setFieldValueByName(Object[] fieldValues, String fieldName, Object value) {
        switch (fieldName) {
            case "id" -> fieldValues[0] = value;
            case "subject" -> fieldValues[1] = value;
            case "from" -> fieldValues[2] = value;
            case "to" -> fieldValues[3] = value;
            case "body" -> fieldValues[4] = value;
            case "date" -> fieldValues[5] = value;
            case "attachments" -> fieldValues[6] = value;
            default -> log.warn("Unknown field name: {}", fieldName);
        }
    }
    
    private String getStringValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        return value != null ? value.toString() : null;
    }
    
    private LocalDateTime getDateValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getListValue(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private List<EmailDocument> documents;
        private long totalHits;
        private String source;
        private long queryTime;
    }
}