package dev.aparikh.searchdifftool.search.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableConfigurationProperties(SolrClientConfigurationProperties.class)
public class SolrConfig {

    @Bean
    SolrClient primaryClient(SolrClientConfigurationProperties config) {
        return createSolrClient(
                config.primary().url(),
                config.primary().core(),
                config.connection());
    }

    @Bean
    SolrClient secondaryClient(SolrClientConfigurationProperties config) {
        return createSolrClient(
                config.secondary().url(),
                config.secondary().core(),
                config.connection());
    }

    private SolrClient createSolrClient(String baseUrl, String core,
                                        SolrClientConfigurationProperties.ConnectionConfig connection) {
        String fullUrl = baseUrl + "/" + core;
        return new HttpSolrClient.Builder(fullUrl)
            .withConnectionTimeout(connection.timeout(), TimeUnit.MILLISECONDS)
            .withSocketTimeout(connection.socketTimeout(), TimeUnit.MILLISECONDS)
            .build();
    }

}