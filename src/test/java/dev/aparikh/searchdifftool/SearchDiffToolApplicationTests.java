package dev.aparikh.searchdifftool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "solr.primary.url=http://localhost:8983/solr",
    "solr.primary.core=test_primary", 
    "solr.secondary.url=http://localhost:8984/solr",
    "solr.secondary.core=test_secondary"
})
class SearchDiffToolApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the Spring context loads successfully
        // Solr clients won't actually connect during context loading
    }

}
