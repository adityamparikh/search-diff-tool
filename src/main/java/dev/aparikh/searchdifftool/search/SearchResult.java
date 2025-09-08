package dev.aparikh.searchdifftool.search;

import org.apache.solr.common.SolrDocument;

import java.util.List;

/**
 * Represents the result of a search operation containing documents, hit counts, and metadata.
 */
public record SearchResult(
    List<SolrDocument> documents,
    long totalHits,
    String source,
    long queryTime
) {}