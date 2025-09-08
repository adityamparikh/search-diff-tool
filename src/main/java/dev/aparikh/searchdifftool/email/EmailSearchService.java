package dev.aparikh.searchdifftool.email;

import dev.aparikh.searchdifftool.email.model.EmailDocument;
import dev.aparikh.searchdifftool.email.model.SearchCriteria;
import dev.aparikh.searchdifftool.solr.client.SolrClientService;
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
        
        return EmailDocument.builder()
            .id(getStringValue(commonDoc, "id"))
            .subject(getStringValue(commonDoc, "subject"))
            .from(getStringValue(commonDoc, "from"))
            .to(getStringValue(commonDoc, "to"))
            .body(getStringValue(commonDoc, "body"))
            .date(getDateValue(commonDoc, "date"))
            .attachments(getListValue(commonDoc, "attachments"))
            .source(isPrimary ? "primary" : "secondary")
            .metadata(commonDoc)
            .build();
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