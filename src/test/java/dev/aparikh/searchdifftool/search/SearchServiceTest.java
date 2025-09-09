package dev.aparikh.searchdifftool.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SolrClient primaryClient;

    @Mock
    private SolrClient secondaryClient;

    @Mock
    private SolrQueryBuilder solrQueryBuilder;

    @Mock
    private QueryResponse queryResponse;

    private SolrDocumentList solrDocumentList;

    @Mock
    private SolrQuery solrQuery;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(primaryClient, secondaryClient, solrQueryBuilder);
        solrDocumentList = new SolrDocumentList();
    }

    @Test
    void searchPrimary_withValidCriteria_shouldReturnSearchResult() throws Exception {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .maxResults(10)
                .build();

        solrDocumentList.setNumFound(5L);
        when(solrQueryBuilder.buildQueryForPrimary(criteria)).thenReturn(solrQuery);
        when(primaryClient.query(solrQuery)).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(queryResponse.getQTime()).thenReturn(100);

        // When
        SearchResult result = searchService.searchPrimary(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalHits()).isEqualTo(5L);
        assertThat(result.source()).isEqualTo("primary");
        assertThat(result.queryTime()).isEqualTo(100);
        assertThat(result.documents()).isNotNull();
    }

    @Test
    void searchSecondary_withValidCriteria_shouldReturnSearchResult() throws Exception {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .maxResults(10)
                .build();

        solrDocumentList.setNumFound(3L);
        when(solrQueryBuilder.buildQueryForSecondary(criteria)).thenReturn(solrQuery);
        when(secondaryClient.query(solrQuery)).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(queryResponse.getQTime()).thenReturn(150);

        // When
        SearchResult result = searchService.searchSecondary(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalHits()).isEqualTo(3L);
        assertThat(result.source()).isEqualTo("secondary");
        assertThat(result.queryTime()).isEqualTo(150);
        assertThat(result.documents()).isNotNull();
    }

    @Test
    void searchPrimary_whenSolrServerException_shouldThrowRuntimeException() throws Exception {
        // Given
        SearchCriteria criteria = SearchCriteria.builder().subject("test").build();
        when(solrQueryBuilder.buildQueryForPrimary(criteria)).thenReturn(solrQuery);
        when(primaryClient.query(solrQuery)).thenThrow(new SolrServerException("Solr error"));

        // When & Then
        assertThatThrownBy(() -> searchService.searchPrimary(criteria))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Primary search failed")
                .hasCauseInstanceOf(SolrServerException.class);
    }

    @Test
    void searchSecondary_whenIOException_shouldThrowRuntimeException() throws Exception {
        // Given
        SearchCriteria criteria = SearchCriteria.builder().subject("test").build();
        when(solrQueryBuilder.buildQueryForSecondary(criteria)).thenReturn(solrQuery);
        when(secondaryClient.query(solrQuery)).thenThrow(new IOException("IO error"));

        // When & Then
        assertThatThrownBy(() -> searchService.searchSecondary(criteria))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Secondary search failed")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void searchPrimary_withEmptyCriteria_shouldReturnSearchResult() throws Exception {
        // Given
        SearchCriteria criteria = SearchCriteria.builder().build();
        
        solrDocumentList.setNumFound(0L);
        when(solrQueryBuilder.buildQueryForPrimary(criteria)).thenReturn(solrQuery);
        when(primaryClient.query(solrQuery)).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(queryResponse.getQTime()).thenReturn(50);

        // When
        SearchResult result = searchService.searchPrimary(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalHits()).isEqualTo(0L);
        assertThat(result.source()).isEqualTo("primary");
        assertThat(result.queryTime()).isEqualTo(50);
    }

    @Test
    void searchSecondary_withEmptyCriteria_shouldReturnSearchResult() throws Exception {
        // Given
        SearchCriteria criteria = SearchCriteria.builder().build();
        
        solrDocumentList.setNumFound(0L);
        when(solrQueryBuilder.buildQueryForSecondary(criteria)).thenReturn(solrQuery);
        when(secondaryClient.query(solrQuery)).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(queryResponse.getQTime()).thenReturn(75);

        // When
        SearchResult result = searchService.searchSecondary(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalHits()).isEqualTo(0L);
        assertThat(result.source()).isEqualTo("secondary");
        assertThat(result.queryTime()).isEqualTo(75);
    }
}