package br.com.vitor.challengesea.model.solicitation;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query parameters of the analyst search; dateFrom and dateTo bound the creation date, inclusive. */
public record SolicitationSearchFilterDTO(
        String q,
        SolicitationStatus status,
        ServiceType serviceType,
        Priority priority,
        String state,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {}
