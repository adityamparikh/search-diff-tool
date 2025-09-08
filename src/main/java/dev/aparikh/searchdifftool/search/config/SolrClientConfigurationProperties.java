package dev.aparikh.searchdifftool.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "solr")
public record SolrClientConfigurationProperties(
    SolrInstanceConfig primary,
    SolrInstanceConfig secondary,
    ConnectionConfig connection
) {
    
    public record SolrInstanceConfig(
        String url,
        String core
    ) {}

    public record ConnectionConfig(
        int timeout,
        int socketTimeout,
        int maxConnections,
        int maxConnectionsPerRoute
    ) {}
}