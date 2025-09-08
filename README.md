# Search Diff Tool

A Spring Boot application for comparing search results between two Apache Solr instances with different schemas. This tool normalizes results from both systems and provides detailed difference analysis including hit counts, missing documents, and content variations.

## 🚀 Features

### Core Capabilities
- **Dual Solr Integration**: Connect to two different Solr instances simultaneously
- **Schema Mapping**: Handle different field names between Solr schemas
- **Comprehensive Diff Analysis**:
  - Hit count comparison
  - Missing document detection (present in one system but not the other)
  - Deep content comparison of matching documents
  - Field-level difference reporting
- **REST API**: Clean JSON API for integration
- **Detailed Reporting**: Structured diff results with metrics and summaries

### Architecture
- **Package-by-Feature**: Modular design for easy extraction of diff engine
- **Spring Boot 3.5.5**: Modern Java 21 application
- **Configurable**: Environment-based configuration for different deployments
- **Testable**: Comprehensive test suite with edge case coverage

## 🛠️ Prerequisites

- **Java 21** or higher
- **Docker** (optional, for integration tests)
- **Apache Solr 9.x** instances (2 separate instances for comparison)

## 📦 Build & Run

### Build the Application

```bash
# Clone the repository
git clone <repository-url>
cd search-diff-tool

# Build the application
./gradlew build

# Run tests (unit tests only, integration tests require Docker)
./gradlew test

# Run integration tests (requires Docker)
./gradlew test -Dspring.profiles.active=integration
```

### Configuration

Configure your Solr instances in `src/main/resources/application.properties`:

```properties
# Primary Solr Instance
solr.primary.url=http://localhost:8983/solr
solr.primary.core=emails_primary

# Secondary Solr Instance  
solr.secondary.url=http://localhost:8984/solr
solr.secondary.core=emails_secondary

# Connection Settings
solr.connection.timeout=5000
solr.socket.timeout=30000
solr.max.connections=100
solr.max.connections.per.route=20

# Search Configuration
search.default.max.results=1000
search.comparison.batch.size=100
```

### Schema Field Mapping

Customize field mappings in `FieldMappingConfig.java`:

```java
// Primary system field mappings
primaryToCommon.put("email_subject", "subject");
primaryToCommon.put("email_from", "from");
primaryToCommon.put("email_to", "to");

// Secondary system field mappings  
secondaryToCommon.put("subject_field", "subject");
secondaryToCommon.put("from_field", "from");
secondaryToCommon.put("to_field", "to");
```

### Run the Application

```bash
# Run with default profile
./gradlew bootRun

# Run with custom configuration
./gradlew bootRun --args='--spring.config.location=classpath:/application-prod.properties'

# Run as JAR
java -jar build/libs/search-diff-tool-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## 📡 API Usage

### Health Check

```bash
GET /api/search-diff/health
```

**Response:**
```json
{
  "success": true,
  "data": "Search Diff Tool is running",
  "message": "Service is healthy",
  "timestamp": "2024-09-07T23:30:00"
}
```

### Search Diff Comparison

```bash
POST /api/search-diff
Content-Type: application/json
```

**Request Body:**
```json
{
  "subject": "important meeting",
  "from": "manager@company.com",
  "dateFrom": "2024-01-01T00:00:00",
  "dateTo": "2024-12-31T23:59:59",
  "maxResults": 100,
  "start": 0,
  "filters": {
    "priority": "high"
  }
}
```

**Request Parameters:**
- `subject` (optional): Email subject search term
- `from` (optional): Sender email address
- `to` (optional): Recipient email address  
- `body` (optional): Email body search term
- `dateFrom` (optional): Start date for search range
- `dateTo` (optional): End date for search range
- `attachmentName` (optional): Attachment filename
- `query` (optional): Raw Solr query string
- `maxResults` (optional): Maximum results to return (1-10000, default: 1000)
- `start` (optional): Starting offset for pagination (default: 0)
- `filters` (optional): Additional search filters as key-value pairs

## 📊 Sample Responses

### Use Case 1: Perfect Match
Both systems return identical results.

**Response:**
```json
{
  "success": true,
  "data": {
    "hitCounts": {
      "primary": 25,
      "secondary": 25,
      "difference": 0
    },
    "countsMatch": true,
    "onlyInPrimary": [],
    "onlyInSecondary": [],
    "contentDifferences": [],
    "summary": "Hit counts match (25). Data matches completely.",
    "metrics": {
      "totalDocumentsCompared": 25,
      "documentsWithDifferences": 0,
      "totalFieldDifferences": 0,
      "queryTimePrimary": 45,
      "queryTimeSecondary": 52,
      "comparisonTimeMs": 123,
      "differencesByField": {}
    }
  },
  "message": "Search diff completed successfully",
  "timestamp": "2024-09-07T23:30:00"
}
```

### Use Case 2: Different Hit Counts
One system returns more results than the other.

**Response:**
```json
{
  "success": true,
  "data": {
    "hitCounts": {
      "primary": 30,
      "secondary": 25,
      "difference": 5
    },
    "countsMatch": false,
    "onlyInPrimary": [
      "email_id_101",
      "email_id_102", 
      "email_id_103",
      "email_id_104",
      "email_id_105"
    ],
    "onlyInSecondary": [],
    "contentDifferences": [],
    "summary": "Hit counts differ by 5 (Primary: 30, Secondary: 25). 5 documents only in primary.",
    "metrics": {
      "totalDocumentsCompared": 25,
      "documentsWithDifferences": 0,
      "totalFieldDifferences": 0,
      "queryTimePrimary": 48,
      "queryTimeSecondary": 43,
      "comparisonTimeMs": 187,
      "differencesByField": {}
    }
  },
  "message": "Search diff completed successfully",
  "timestamp": "2024-09-07T23:30:00"
}
```

### Use Case 3: Same Hit Counts, Different Content
Both systems return same number of documents but content differs.

**Response:**
```json
{
  "success": true,
  "data": {
    "hitCounts": {
      "primary": 20,
      "secondary": 20,
      "difference": 0
    },
    "countsMatch": true,
    "onlyInPrimary": [],
    "onlyInSecondary": [],
    "contentDifferences": [
      {
        "documentId": "email_123",
        "field": "subject",
        "primaryValue": "Quarterly Meeting - Q4 2024",
        "secondaryValue": "Quarterly Meeting - Q4",
        "differenceType": "DIFFERENT"
      },
      {
        "documentId": "email_124",
        "field": "from",
        "primaryValue": "john.doe@company.com",
        "secondaryValue": "j.doe@company.com",
        "differenceType": "DIFFERENT"
      },
      {
        "documentId": "email_125",
        "field": "attachments",
        "primaryValue": ["report.pdf", "slides.pptx"],
        "secondaryValue": null,
        "differenceType": "MISSING_IN_SECONDARY"
      }
    ],
    "summary": "Hit counts match (20). Found 3 field differences across 3 documents.",
    "metrics": {
      "totalDocumentsCompared": 20,
      "documentsWithDifferences": 3,
      "totalFieldDifferences": 3,
      "queryTimePrimary": 67,
      "queryTimeSecondary": 71,
      "comparisonTimeMs": 245,
      "differencesByField": {
        "subject": 1,
        "from": 1,
        "attachments": 1
      }
    }
  },
  "message": "Search diff completed successfully",
  "timestamp": "2024-09-07T23:30:00"
}
```

### Use Case 4: Complex Differences
Mixed scenario with different hit counts, missing documents, and content differences.

**Response:**
```json
{
  "success": true,
  "data": {
    "hitCounts": {
      "primary": 28,
      "secondary": 26,
      "difference": 2
    },
    "countsMatch": false,
    "onlyInPrimary": [
      "email_201",
      "email_202"
    ],
    "onlyInSecondary": [
      "email_301"
    ],
    "contentDifferences": [
      {
        "documentId": "email_150",
        "field": "subject",
        "primaryValue": "Project Update - Phase 1 Complete",
        "secondaryValue": "Project Update - Phase 1 Done",
        "differenceType": "DIFFERENT"
      },
      {
        "documentId": "email_151", 
        "field": "body",
        "primaryValue": "Dear team,\n\nThe project milestone has been achieved...",
        "secondaryValue": "Hi team,\n\nWe've completed the milestone...",
        "differenceType": "DIFFERENT"
      },
      {
        "documentId": "email_152",
        "field": "date",
        "primaryValue": "2024-09-01T14:30:00",
        "secondaryValue": null,
        "differenceType": "MISSING_IN_SECONDARY"
      }
    ],
    "summary": "Hit counts differ by 2 (Primary: 28, Secondary: 26). 2 documents only in primary. 1 documents only in secondary. Found 3 field differences across 3 documents.",
    "metrics": {
      "totalDocumentsCompared": 25,
      "documentsWithDifferences": 3,
      "totalFieldDifferences": 3,
      "queryTimePrimary": 89,
      "queryTimeSecondary": 76,
      "comparisonTimeMs": 312,
      "differencesByField": {
        "subject": 1,
        "body": 1,
        "date": 1
      }
    }
  },
  "message": "Search diff completed successfully",
  "timestamp": "2024-09-07T23:30:00"
}
```

### Error Response
When validation fails or an error occurs.

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "timestamp": "2024-09-07T23:30:00"
}
```

**Response (500 Internal Server Error):**
```json
{
  "success": false,
  "error": "DIFF_ERROR", 
  "message": "Failed to perform search diff: Connection timeout to primary Solr instance",
  "timestamp": "2024-09-07T23:30:00"
}
```

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test --tests="*Test" --continue
```

### Run Integration Tests (Requires Docker)
```bash
# Enable integration tests by removing @Disabled annotation
./gradlew test --tests="*IntegrationTest*"
```

### Test Coverage
- **DiffService**: 5 comprehensive scenarios including edge cases
- **Controllers**: REST endpoint validation and error handling
- **Schema Mapping**: Field mapping between different schemas
- **Comparators**: Hit count, content, and result set comparison logic

## 🏗️ Architecture

### Package Structure
```
src/main/java/dev/aparikh/searchdifftool/
├── diff/                    # Extractable diff engine
│   ├── DiffService.java
│   ├── model/DiffResult.java
│   └── comparison/          # Comparison algorithms
├── solr/                    # Solr-specific operations  
│   ├── client/             # Connection management
│   └── schema/             # Field mapping
├── email/                   # Email domain models
├── api/                     # REST controllers
└── config/                  # Application configuration
```

### Key Components

**DiffService**: Main orchestrator that coordinates the comparison process
**SchemaMapper**: Handles field name translation between different schemas  
**Comparators**: Specialized comparison logic for different aspects
- `HitCountComparator`: Compares total result counts
- `ResultSetComparator`: Finds documents present in one system but not the other  
- `ContentComparator`: Deep comparison of document field values

## 🔧 Customization

### Adding New Field Mappings
Edit `FieldMappingConfig.java` to add new field mappings:

```java
// Add new primary mappings
primaryToCommon.put("custom_field", "normalized_field");

// Add new secondary mappings  
secondaryToCommon.put("different_custom_field", "normalized_field");
```

### Custom Search Criteria
Extend `SearchCriteria.java` to add new search parameters:

```java
private String customField;
private List<String> tags;
```

### Extending Diff Logic
The diff engine is designed for extensibility. Create new comparators by implementing comparison interfaces:

```java
@Component
public class CustomComparator {
    public CustomResult compare(List<EmailDocument> primary, List<EmailDocument> secondary) {
        // Custom comparison logic
    }
}
```

## 📈 Performance Considerations

- **Batch Size**: Configure `search.comparison.batch.size` for optimal memory usage
- **Connection Pooling**: Adjust Solr connection pool settings based on load
- **Result Limits**: Use `maxResults` parameter to control comparison scope
- **Pagination**: Use `start` parameter for large result sets

## 🚦 Monitoring

The application provides several monitoring endpoints:

- **Health Check**: `/api/search-diff/health`
- **Spring Actuator**: `/actuator/health`, `/actuator/metrics`
- **Logs**: Comprehensive logging at DEBUG level for troubleshooting

## 🔒 Security

- **Input Validation**: Request parameters are validated using Bean Validation
- **Error Handling**: Sanitized error responses without sensitive information
- **No Authentication**: Add Spring Security if authentication is required

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)  
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 💡 Future Enhancements

- **Async Processing**: Support for large-scale comparisons with job queues
- **Export Formats**: Export diff results to CSV, Excel, or PDF
- **Real-time Monitoring**: WebSocket-based real-time diff monitoring
- **UI Dashboard**: Web interface for visual diff analysis
- **Multi-Schema Support**: Support for more than two Solr instances
- **Advanced Filtering**: Complex query building with AND/OR logic