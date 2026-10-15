package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.audit.Identifiable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SolicitationResponseDTO(
        Long id,
        Long ownerId,
        SolicitationStatus status,
        int currentStep,
        ServiceType serviceType,
        String title,
        String description,
        String cep,
        boolean cepValidated,
        String street,
        String neighborhood,
        String city,
        String state,
        String number,
        String complement,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        boolean termsAccepted,
        Long analystId,
        String decisionComment,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant decidedAt,
        List<PendingFieldDTO> pendingFields)
        implements Identifiable {}
