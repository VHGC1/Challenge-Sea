package br.com.vitor.challengesea.model.solicitation;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Step 3. The HIGH-priority minimum value is a completeness rule on {@link Solicitation}. */
public record ConfirmationDTO(
        Priority priority,
        @FutureOrPresent LocalDate preferredDate,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal estimatedValue,
        Boolean termsAccepted) {}
