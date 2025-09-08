package dev.aparikh.searchdifftool.normalization;

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
    private FieldMappingConfigurationProperties fieldMappingConfigurationProperties;

    private NormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new NormalizationService(fieldMappingConfigurationProperties);
    }

    @Test
    void normalize_withValidSolrDocument_shouldReturnNormalizedDocument() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("email_id", "doc1");
        solrDoc.put("email_subject", "Test Subject");
        solrDoc.put("email_from", "test@example.com");
        solrDoc.put("email_to", "recipient@example.com");
        solrDoc.put("email_body", "Test body");
        solrDoc.put("email_date", new Date());
        solrDoc.put("email_attachments", Arrays.asList("file1.txt", "file2.pdf"));

        // Create field mappings configuration
        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("subject", createFieldMapping("email_subject", "subject_field", "string"));
        fieldMappings.put("from", createFieldMapping("email_from", "from_field", "string"));
        fieldMappings.put("to", createFieldMapping("email_to", "to_field", "string"));
        fieldMappings.put("body", createFieldMapping("email_body", "body_field", "string"));
        fieldMappings.put("date", createFieldMapping("email_date", "date_field", "date"));
        fieldMappings.put("attachments", createFieldMapping("email_attachments", "attachment_list", "list"));

        // Set up primary to common mapping 
        Map<String, String> primaryMapping = new HashMap<>();
        primaryMapping.put("email_id", "id");
        primaryMapping.put("email_subject", "subject");
        primaryMapping.put("email_from", "from");
        primaryMapping.put("email_to", "to");
        primaryMapping.put("email_body", "body");
        primaryMapping.put("email_date", "date");
        primaryMapping.put("email_attachments", "attachments");

        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, true);

        // Then
        assertThat(result.getId()).isEqualTo("doc1");
        assertThat(result.getSubject()).isEqualTo("Test Subject");
        assertThat(result.getFrom()).isEqualTo("test@example.com");
        assertThat(result.getTo()).isEqualTo("recipient@example.com");
        assertThat(result.getBody()).isEqualTo("Test body");
        assertThat(result.getDate()).isInstanceOf(LocalDateTime.class);
        assertThat(result.getAttachments()).hasSize(2).contains("file1.txt", "file2.pdf");
        assertThat(result.getSource()).isEqualTo("primary");
    }

    @Test
    void normalize_withSecondarySource_shouldSetCorrectSource() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("message_id", "doc1");

        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("subject", createFieldMapping("email_subject", "subject_field", "string"));
        fieldMappings.put("from", createFieldMapping("email_from", "from_field", "string"));
        fieldMappings.put("to", createFieldMapping("email_to", "to_field", "string"));
        fieldMappings.put("body", createFieldMapping("email_body", "body_field", "string"));
        fieldMappings.put("date", createFieldMapping("email_date", "date_field", "date"));
        fieldMappings.put("attachments", createFieldMapping("email_attachments", "attachment_list", "list"));

        // Set up secondary to common mapping
        Map<String, String> secondaryMapping = new HashMap<>();
        secondaryMapping.put("message_id", "id");

        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);
        when(fieldMappingConfigurationProperties.getSecondaryToCommon()).thenReturn(secondaryMapping);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, false);

        // Then
        assertThat(result.getSource()).isEqualTo("secondary");
        assertThat(result.getId()).isEqualTo("doc1");
    }

    @Test
    void normalize_withUnknownDataType_shouldTreatAsString() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("email_id", "doc1");
        solrDoc.put("custom_primary", "custom_value");

        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("subject", createFieldMapping("email_subject", "subject_field", "string"));
        fieldMappings.put("from", createFieldMapping("email_from", "from_field", "string"));
        fieldMappings.put("to", createFieldMapping("email_to", "to_field", "string"));
        fieldMappings.put("body", createFieldMapping("email_body", "body_field", "string"));
        fieldMappings.put("date", createFieldMapping("email_date", "date_field", "date"));
        fieldMappings.put("attachments", createFieldMapping("email_attachments", "attachment_list", "list"));
        fieldMappings.put("custom_field", createFieldMapping("custom_primary", "custom_secondary", "unknown_type"));

        // Set up primary to common mapping
        Map<String, String> primaryMapping = new HashMap<>();
        primaryMapping.put("email_id", "id");
        primaryMapping.put("custom_primary", "custom_field");

        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, true);

        // Then
        assertThat(result.getId()).isEqualTo("doc1");
        // The unknown field should be available in metadata
        assertThat(result.getMetadata()).containsEntry("custom_field", "custom_value");
    }

    @Test
    void normalize_withNullValues_shouldHandleGracefully() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("email_id", "doc1");
        // Note: Not adding subject, date, and attachments fields to simulate null values

        Map<String, FieldMappingConfigurationProperties.FieldMapping> fieldMappings = new HashMap<>();
        fieldMappings.put("id", createFieldMapping("email_id", "message_id", "string"));
        fieldMappings.put("subject", createFieldMapping("email_subject", "subject_field", "string"));
        fieldMappings.put("from", createFieldMapping("email_from", "from_field", "string"));
        fieldMappings.put("to", createFieldMapping("email_to", "to_field", "string"));
        fieldMappings.put("body", createFieldMapping("email_body", "body_field", "string"));
        fieldMappings.put("date", createFieldMapping("email_date", "date_field", "date"));
        fieldMappings.put("attachments", createFieldMapping("email_attachments", "attachment_list", "list"));

        // Set up primary to common mapping
        Map<String, String> primaryMapping = new HashMap<>();
        primaryMapping.put("email_id", "id");
        primaryMapping.put("email_subject", "subject");
        primaryMapping.put("email_date", "date");
        primaryMapping.put("email_attachments", "attachments");

        when(fieldMappingConfigurationProperties.getFields()).thenReturn(fieldMappings);
        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        NormalizedDocument result = normalizationService.normalize(solrDoc, true);

        // Then
        assertThat(result.getId()).isEqualTo("doc1");
        assertThat(result.getSubject()).isNull();
        assertThat(result.getDate()).isNull();
        assertThat(result.getAttachments()).isEmpty(); // Should return empty list for null list values
    }

    @Test
    void mapDocumentToCommon_withPrimaryDocument_shouldMapFieldsCorrectly() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("email_id", "doc1");
        solrDoc.put("email_subject", "Test Subject");
        solrDoc.put("email_from", "test@example.com");
        solrDoc.put("unmapped_field", "unmapped_value");

        Map<String, String> primaryMapping = new HashMap<>();
        primaryMapping.put("email_id", "id");
        primaryMapping.put("email_subject", "subject");
        primaryMapping.put("email_from", "from");

        when(fieldMappingConfigurationProperties.getPrimaryToCommon()).thenReturn(primaryMapping);

        // When
        Map<String, Object> result = normalizationService.mapDocumentToCommon(solrDoc, true);

        // Then
        assertThat(result).hasSize(4); // 3 mapped + 1 unmapped
        assertThat(result.get("id")).isEqualTo("doc1");
        assertThat(result.get("subject")).isEqualTo("Test Subject");
        assertThat(result.get("from")).isEqualTo("test@example.com");
        assertThat(result.get("unmapped_field")).isEqualTo("unmapped_value"); // Unmapped fields should be included
    }

    @Test
    void mapDocumentToCommon_withSecondaryDocument_shouldMapFieldsCorrectly() {
        // Given
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.put("message_id", "doc1");
        solrDoc.put("subject_field", "Test Subject");
        solrDoc.put("from_field", "test@example.com");

        Map<String, String> secondaryMapping = new HashMap<>();
        secondaryMapping.put("message_id", "id");
        secondaryMapping.put("subject_field", "subject");
        secondaryMapping.put("from_field", "from");

        when(fieldMappingConfigurationProperties.getSecondaryToCommon()).thenReturn(secondaryMapping);

        // When
        Map<String, Object> result = normalizationService.mapDocumentToCommon(solrDoc, false);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get("id")).isEqualTo("doc1");
        assertThat(result.get("subject")).isEqualTo("Test Subject");
        assertThat(result.get("from")).isEqualTo("test@example.com");
    }

    private FieldMappingConfigurationProperties.FieldMapping createFieldMapping(String primary, String secondary, String type) {
        return new FieldMappingConfigurationProperties.FieldMapping(primary, secondary, type);
    }
}