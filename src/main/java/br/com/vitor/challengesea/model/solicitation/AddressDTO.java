package br.com.vitor.challengesea.model.solicitation;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Step 2. Street, neighborhood, city and state come from ViaCEP, never from the client. */
public record AddressDTO(
        @Pattern(regexp = "\\d{8}", message = "deve ter 8 dígitos")
        String cep,

        @Size(min = 1, max = 20) String number,
        @Size(max = 120) String complement) {

    public AddressDTO {
        cep = cep == null ? null : cep.replace("-", "").strip();
        number = Texts.strip(number);
        complement = Texts.strip(complement);
    }
}
