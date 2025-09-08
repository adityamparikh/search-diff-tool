package dev.aparikh.searchdifftool.solr.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "solr")
public class SolrClientConfig {
    
    private SolrInstanceConfig primary = new SolrInstanceConfig();
    private SolrInstanceConfig secondary = new SolrInstanceConfig();
    private ConnectionConfig connection = new ConnectionConfig();
    
    @Data
    public static class SolrInstanceConfig {
        private String url;
        private String core;
    }
    
    @Data
    public static class ConnectionConfig {
        private int timeout = 5000;
        private int socketTimeout = 30000;
        private int maxConnections = 100;
        private int maxConnectionsPerRoute = 20;
    }
}