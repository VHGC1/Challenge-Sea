package br.com.vitor.challengesea.model.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

public record CoveredStatesDTO(
        @NotNull Set<@Pattern(regexp = "[A-Z]{2}", message = "deve ser uma UF, como SP") String> states) {}
