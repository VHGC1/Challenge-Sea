package br.com.vitor.challengesea.model.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** An internal user created by an ADMIN; {@code coveredStates} only applies to an ANALYST. */
public record CreateUserDTO(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Set<@Pattern(regexp = "[A-Z]{2}", message = "deve ser uma UF, como SP") String> coveredStates) {}
