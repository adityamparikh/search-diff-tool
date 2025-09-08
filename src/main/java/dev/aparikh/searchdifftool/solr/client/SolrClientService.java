package dev.aparikh.searchdifftool.solr.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SolrClientService {
    
    private final SolrClient primaryClient;
    private final SolrClient secondaryClient;
    private final SolrClientConfig config;
    
    public SolrClientService(SolrClientConfig config) {
        this.config = config;
        this.primaryClient = createSolrClient(
            config.getPrimary().getUrl(),
            config.getPrimary().getCore()
        );
        this.secondaryClient = createSolrClient(
            config.getSecondary().getUrl(),
            config.getSecondary().getCore()
        );
        log.info("Initialized Solr clients for primary: {} and secondary: {}", 
            config.getPrimary().getUrl(), config.getSecondary().getUrl());
    }
    
    private SolrClient createSolrClient(String baseUrl, String core) {
        String fullUrl = baseUrl + "/" + core;
        return new HttpSolrClient.Builder(fullUrl)
            .withConnectionTimeout(config.getConnection().getTimeout())
            .withSocketTimeout(config.getConnection().getSocketTimeout())
            .build();
    }
    
    public SolrClient getPrimaryClient() {
        return primaryClient;
    }
    
    public SolrClient getSecondaryClient() {
        return secondaryClient;
    }
    
    public String getPrimaryCore() {
        return config.getPrimary().getCore();
    }
    
    public String getSecondaryCore() {
        return config.getSecondary().getCore();
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            if (primaryClient != null) {
                primaryClient.close();
            }
            if (secondaryClient != null) {
                secondaryClient.close();
            }
            log.info("Closed Solr clients");
        } catch (Exception e) {
            log.error("Error closing Solr clients", e);
        }
    }
}