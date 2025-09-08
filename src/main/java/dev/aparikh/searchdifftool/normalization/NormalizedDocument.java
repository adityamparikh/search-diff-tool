package dev.aparikh.searchdifftool.normalization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record NormalizedDocument(
    String id,
    String subject,
    String from,
    String to,
    String body,
    LocalDateTime date,
    List<String> attachments,
    Map<String, Object> metadata,
    String source // For comparison purposes - "primary" or "secondary"
) {}