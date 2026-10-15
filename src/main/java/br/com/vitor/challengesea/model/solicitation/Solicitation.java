package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.model.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A client's service request. While DRAFT it may be incomplete; the constraints below, grouped by
 * {@link SolicitationStep}, say what each step needs to count as done and are all checked on submit.
 * Format rules (lengths, CEP digits) are enforced earlier, on the step request DTOs.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitations")
public class Solicitation {
    public static final BigDecimal HIGH_PRIORITY_MIN_VALUE = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    private SolicitationStatus status = SolicitationStatus.DRAFT;

    private int currentStep = 1;

    @NotNull(groups = SolicitationStep.BasicData.class)
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @NotBlank(groups = SolicitationStep.BasicData.class)
    private String title;

    @NotBlank(groups = SolicitationStep.BasicData.class)
    private String description;

    @NotBlank(groups = SolicitationStep.Address.class)
    private String cep;

    /** True only when ViaCEP answered with an address for {@link #cep}. */
    @AssertTrue(
            groups = SolicitationStep.Address.class,
            message = "CEP não confirmado no ViaCEP (inválido ou serviço indisponível)")
    private boolean cepValidated;

    private String street;

    private String neighborhood;

    private String city;

    private String state;

    @NotBlank(groups = SolicitationStep.Address.class)
    private String number;

    private String complement;

    @NotNull(groups = SolicitationStep.Confirmation.class)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @NotNull(groups = SolicitationStep.Confirmation.class)
    @FutureOrPresent(groups = SolicitationStep.Confirmation.class)
    private LocalDate preferredDate;

    @NotNull(groups = SolicitationStep.Confirmation.class)
    @PositiveOrZero(groups = SolicitationStep.Confirmation.class)
    private BigDecimal estimatedValue;

    @AssertTrue(groups = SolicitationStep.Confirmation.class, message = "os termos devem ser aceitos")
    private boolean termsAccepted;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser analyst;

    private String decisionComment;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant submittedAt;

    private Instant decidedAt;

    @Version
    private long version;

    @AssertTrue(
            groups = SolicitationStep.Confirmation.class,
            message = "com prioridade HIGH o valor estimado deve ser no mínimo 100")
    public boolean isEstimatedValueAllowedForPriority() {
        return priority != Priority.HIGH
                || estimatedValue == null
                || estimatedValue.compareTo(HIGH_PRIORITY_MIN_VALUE) >= 0;
    }
}
