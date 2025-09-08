package dev.aparikh.searchdifftool;

import dev.aparikh.searchdifftool.search.solr.SolrClientConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SolrClientConfigurationProperties.class)
public class SearchDiffToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchDiffToolApplication.class, args);
    }

}
