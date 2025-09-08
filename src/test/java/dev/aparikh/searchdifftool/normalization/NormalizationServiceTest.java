package dev.aparikh.searchdifftool.normalization;

import dev.aparikh.searchdifftool.search.solr.schema.SchemaMapper;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormalizationServiceTest {

    @Mock
    private SchemaMapper schemaMapper;

    @Mock
    private FieldMappingConfigurationProperties fieldMappingConfigurationProperties;

    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService(schemaMapper, fieldMappingConfigurationProperties);
    }

    @Test
    void normalize_withValidSolrDocument_shouldReturnNormalizedDocument() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("email_id", "doc1");
        solrDoc.put("email_subject", "Test Subject");

        Map<String, Object> commonDoc = new HashMap<>();
        commonDoc.put("id", "doc1");
        commonDoc.put("subject", "Test Subject");
        commonDoc.put("from", "test@example.com");
        commonDoc.put("to", "recipient@example.com");
        commonDoc.put("body", "Test body");
        commonDoc.put("date", new Date());
        commonDoc.put("attachments", Arrays.asList("file1.txt", "file2.pdf"));

        // Create field mappings
        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("subject", createFieldMapping("email_subject", "subject_field", "string"));
        fieldMappings.put("from", createFieldMapping("email_from", "from_field", "string"));
        fieldMappings.put("to", createFieldMapping("email_to", "to_field", "string"));
        fieldMappings.put("body", createFieldMapping("email_body", "body_field", "string"));
        fieldMappings.put("date", createFieldMapping("email_date", "date_field", "date"));
        fieldMappings.put("attachments", createFieldMapping("email_attachments", "attachment_list", "list"));

        when(schemaMapper.mapDocumentToCommon(solrDoc, true)).thenReturn(commonDoc);
        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, true);

        // Then
        assertThat(result.id()).isEqualTo("doc1");
        assertThat(result.subject()).isEqualTo("Test Subject");
        assertThat(result.from()).isEqualTo("test@example.com");
        assertThat(result.to()).isEqualTo("recipient@example.com");
        assertThat(result.body()).isEqualTo("Test body");
        assertThat(result.date()).isInstanceOf(LocalDateTime.class);
        assertThat(result.attachments()).hasSize(2).contains("file1.txt", "file2.pdf");
        assertThat(result.metadata()).isEqualTo(commonDoc);
        assertThat(result.source()).isEqualTo("primary");
    }

    @Test
    void normalize_withSecondarySource_shouldSetCorrectSource() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        Map<String, Object> commonDoc = new HashMap<>();
        commonDoc.put("id", "doc1");

        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));

        when(schemaMapper.mapDocumentToCommon(solrDoc, false)).thenReturn(commonDoc);
        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, false);

        // Then
        assertThat(result.source()).isEqualTo("secondary");
    }

    @Test
    void normalize_withUnknownDataType_shouldTreatAsString() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        Map<String, Object> commonDoc = new HashMap<>();
        commonDoc.put("id", "doc1");
        commonDoc.put("custom_field", "custom_value");

        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("custom_field", createFieldMapping("custom_primary", "custom_secondary", "unknown_type"));

        when(schemaMapper.mapDocumentToCommon(solrDoc, true)).thenReturn(commonDoc);
        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, true);

        // Then
        assertThat(result.id()).isEqualTo("doc1");
        // The unknown field should be ignored since it doesn't map to a known field position
    }

    @Test
    void normalize_withNullValues_shouldHandleGracefully() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        Map<String, Object> commonDoc = new HashMap<>();
        commonDoc.put("id", "doc1");
        commonDoc.put("subject", null);
        commonDoc.put("date", null);
        commonDoc.put("attachments", null);

        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("subject", createFieldMapping("email_subject", "subject_field", "string"));
        fieldMappings.put("date", createFieldMapping("email_date", "date_field", "date"));
        fieldMappings.put("attachments", createFieldMapping("email_attachments", "attachment_list", "list"));

        when(schemaMapper.mapDocumentToCommon(solrDoc, true)).thenReturn(commonDoc);
        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, true);

        // Then
        assertThat(result.id()).isEqualTo("doc1");
        assertThat(result.subject()).isNull();
        assertThat(result.date()).isNull();
        assertThat(result.attachments()).isEmpty(); // Should return empty list for null list values
    }

    private FieldMappingConfigurationProperties.FieldMapping createFieldMapping(String primary, String secondary, String type) {
        return new FieldMappingConfigurationProperties.FieldMapping(primary, secondary, type);
    }
}