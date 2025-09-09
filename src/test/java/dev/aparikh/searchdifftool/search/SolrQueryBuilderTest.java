package dev.aparikh.searchdifftool.search;

import dev.aparikh.searchdifftool.normalization.FieldMappingConfigurationProperties;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolrQueryBuilderTest {

    @Mock
    private FieldMappingConfigurationProperties fieldMappingConfigurationProperties;

    private SolrQueryBuilder solrQueryBuilder;

    @BeforeEach
    void setUp() {
        solrQueryBuilder = new SolrQueryBuilder(fieldMappingConfigurationProperties);
    }

    @Test
    void buildQueryForPrimary_withRawQuery_shouldUseRawQuery() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .query("subject:test AND from:john")
                .maxResults(50)
                .start(10)
                .build();

        Map<String, String> primaryMapping = Map.of(
                "email_subject", "subject",
                "email_from", "from"
        );
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForPrimary(criteria);

        // Then
        assertThat(result.getQuery()).isEqualTo("subject:test AND from:john");
        assertThat(result.getRows()).isEqualTo(50);
        assertThat(result.getStart()).isEqualTo(10);
    }

    @Test
    void buildQueryForSecondary_withRawQuery_shouldUseRawQuery() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .query("title:test")
                .maxResults(25)
                .start(0)
                .build();

        Map<String, String> secondaryMapping = Map.of(
                "title_field", "subject",
                "sender_field", "from"
        );
        when(fieldMappingConfigurationProperties.getSecondaryToCommon()).thenReturn(secondaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForSecondary(criteria);

        // Then
        assertThat(result.getQuery()).isEqualTo("title:test");
        assertThat(result.getRows()).isEqualTo(25);
        assertThat(result.getStart()).isEqualTo(0);
    }

    @Test
    void buildQueryForPrimary_withStructuredCriteria_shouldBuildMappedQuery() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test email")
                .from("john@example.com")
                .maxResults(100)
                .start(0)
                .build();

        Map<String, String> primaryMapping = Map.of(
                "email_subject", "subject",
                "email_from", "from"
        );
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForPrimary(criteria);

        // Then
        String query = result.getQuery();
        assertThat(query).contains("email_subject:\"test email\""); // Quoted because it contains space
        assertThat(query).contains("email_from:john@example.com");
        assertThat(query).contains(" AND ");
        assertThat(result.getRows()).isEqualTo(100);
        assertThat(result.getStart()).isEqualTo(0);
    }

    @Test
    void buildQueryForSecondary_withStructuredCriteria_shouldBuildMappedQuery() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("important")
                .body("meeting")
                .maxResults(50)
                .start(20)
                .build();

        Map<String, String> secondaryMapping = Map.of(
                "subject_field", "subject",
                "body_field", "body"
        );
        when(fieldMappingConfigurationProperties.getSecondaryToCommon()).thenReturn(secondaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForSecondary(criteria);

        // Then
        String query = result.getQuery();
        assertThat(query).contains("subject_field:important");
        assertThat(query).contains("body_field:meeting");
        assertThat(query).contains(" AND ");
        assertThat(result.getRows()).isEqualTo(50);
        assertThat(result.getStart()).isEqualTo(20);
    }

    @Test
    void buildQueryForPrimary_withEmptyCriteria_shouldReturnMatchAllQuery() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .maxResults(10)
                .start(0)
                .build();

        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(new HashMap<>());

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForPrimary(criteria);

        // Then
        assertThat(result.getQuery()).isEqualTo("*:*");
        assertThat(result.getRows()).isEqualTo(10);
        assertThat(result.getStart()).isEqualTo(0);
    }

    @Test
    void buildQueryForPrimary_withUnmappedField_shouldUseFieldAsIs() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("test")
                .maxResults(10)
                .build();

        // Empty mapping - field should be used as-is
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(new HashMap<>());

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForPrimary(criteria);

        // Then
        assertThat(result.getQuery()).contains("subject:test");
    }

    @Test
    void buildQueryForSecondary_withMultipleFields_shouldCombineWithAND() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .subject("quarterly report")
                .from("manager@company.com")
                .to("team@company.com")
                .maxResults(75)
                .build();

        Map<String, String> secondaryMapping = Map.of(
                "subj_field", "subject",
                "sender_field", "from",
                "recipient_field", "to"
        );
        when(fieldMappingConfigurationProperties.getSecondaryToCommon()).thenReturn(secondaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForSecondary(criteria);

        // Then
        String query = result.getQuery();
        assertThat(query).contains("subj_field:\"quarterly report\"");
        assertThat(query).contains("sender_field:manager@company.com");
        assertThat(query).contains("recipient_field:team@company.com");
        // Should have 2 AND operators (3 fields = 2 AND operators)
        assertThat(query.split(" AND ")).hasSize(3);
    }

    @Test
    void buildQueryForPrimary_withEmptyRawQuery_shouldBuildStructuredQuery() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .query("   ") // Empty/whitespace query
                .subject("fallback")
                .maxResults(10)
                .build();

        Map<String, String> primaryMapping = Map.of("email_subject", "subject");
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForPrimary(criteria);

        // Then
        assertThat(result.getQuery()).contains("email_subject:fallback");
        assertThat(result.getQuery()).doesNotContain("   ");
    }

    @Test
    void buildQueryForSecondary_withDateCriteria_shouldIncludeDateFields() {
        // Given
        LocalDateTime dateFrom = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2023, 12, 31, 23, 59);
        
        SearchCriteria criteria = SearchCriteria.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .maxResults(100)
                .build();

        Map<String, String> secondaryMapping = Map.of(
                "date_field", "dateFrom",
                "date_to_field", "dateTo"
        );
        when(fieldMappingConfigurationProperties.getSecondaryToCommon()).thenReturn(secondaryMapping);

        // When
        SolrQuery result = solrQueryBuilder.buildQueryForSecondary(criteria);

        // Then
        String query = result.getQuery();
        assertThat(query).contains("date_field:");
        assertThat(query).contains("date_to_field:");
    }
}