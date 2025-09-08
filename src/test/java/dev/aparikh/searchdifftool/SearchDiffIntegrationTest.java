package dev.aparikh.searchdifftool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aparikh.searchdifftool.api.SearchRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("Requires Docker containers - enable for full integration testing")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
class SearchDiffIntegrationTest {

    @Container
    static SolrContainer primarySolr = new SolrContainer(DockerImageName.parse("solr:9.7.0"))
            .withCollection("emails_primary");

    @Container
    static SolrContainer secondarySolr = new SolrContainer(DockerImageName.parse("solr:9.7.0"))
            .withCollection("emails_secondary");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void configureSolr(DynamicPropertyRegistry registry) {
        registry.add("solr.primary.url", () -> "http://" + primarySolr.getHost() + ":" + primarySolr.getSolrPort() + "/solr");
        registry.add("solr.primary.core", () -> "emails_primary");
        registry.add("solr.secondary.url", () -> "http://" + secondarySolr.getHost() + ":" + secondarySolr.getSolrPort() + "/solr");
        registry.add("solr.secondary.core", () -> "emails_secondary");
    }

    @BeforeAll
    static void setUp() {
        // Containers are already started by @Testcontainers
        // Add test data setup here if needed
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/search-diff/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").value("Search Diff Tool is running"));
    }

    @Test
    void testSearchDiffEndpoint() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .subject("test")
                .maxResults(10)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/search-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.hitCounts").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.summary").exists());
    }

    @Test
    void testSearchDiffWithInvalidRequest() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .maxResults(-1) // Invalid max results
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/search-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}