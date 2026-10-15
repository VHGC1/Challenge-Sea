package br.com.vitor.challengesea.model.solicitation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One search hit, read from the Elasticsearch index. */
public record SolicitationSummaryDTO(
        Long id,
        SolicitationStatus status,
        ServiceType serviceType,
        Priority priority,
        String title,
        String description,
        String city,
        String state,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        Instant createdAt,
        Instant submittedAt) {}
