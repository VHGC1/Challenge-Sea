package br.com.vitor.challengesea.model.solicitation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionDTO(
        @NotNull Decision decision,
        @NotBlank @Size(min = 10, max = 1000) String comment) {

    public DecisionDTO {
        comment = Texts.strip(comment);
    }
}
