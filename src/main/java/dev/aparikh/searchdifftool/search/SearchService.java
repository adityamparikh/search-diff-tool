package dev.aparikh.searchdifftool.search;

import dev.aparikh.searchdifftool.search.solr.schema.SchemaMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SearchService {
    
    private final SolrClient primaryClient;
    private final SolrClient secondaryClient;
    private final SchemaMapper schemaMapper;
    
    public SearchService(SolrClient primaryClient,
                         SolrClient secondaryClient,
                         SchemaMapper schemaMapper) {
        this.primaryClient = primaryClient;
        this.secondaryClient = secondaryClient;
        this.schemaMapper = schemaMapper;
    }
    
    public SearchResult searchPrimary(SearchCriteria criteria) {
        try {
            SolrQuery query = schemaMapper.mapQueryForPrimary(criteria.toQueryMap());
            query.setRows(criteria.getMaxResults());
            query.setStart(criteria.getStart());
            
            QueryResponse response = primaryClient.query(query);
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
            
            QueryResponse response = secondaryClient.query(query);
            return convertToSearchResult(response, false);
            
        } catch (SolrServerException | IOException e) {
            log.error("Error searching secondary Solr instance", e);
            throw new RuntimeException("Secondary search failed", e);
        }
    }
    
    private SearchResult convertToSearchResult(QueryResponse response, boolean isPrimary) {
        SolrDocumentList docs = response.getResults();
        List<SolrDocument> solrDocuments = new ArrayList<>(docs);
        
        return new SearchResult(
            solrDocuments,
            docs.getNumFound(),
            isPrimary ? "primary" : "secondary",
            response.getQTime()
        );
    }
}