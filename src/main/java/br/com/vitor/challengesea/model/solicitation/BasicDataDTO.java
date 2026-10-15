package br.com.vitor.challengesea.model.solicitation;

import jakarta.validation.constraints.Size;

/** Step 1. Every field is optional on a draft save; what is sent must be well-formed. */
public record BasicDataDTO(
        ServiceType serviceType,
        @Size(min = 3, max = 80) String title,
        @Size(min = 20, max = 1000) String description) {

    public BasicDataDTO {
        title = Texts.strip(title);
        description = Texts.strip(description);
    }
}
