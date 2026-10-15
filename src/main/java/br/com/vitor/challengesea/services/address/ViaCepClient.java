package br.com.vitor.challengesea.services.address;

import br.com.vitor.challengesea.model.address.ViaCepAddressDTO;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Looks a CEP up on ViaCEP. An unknown CEP and an unreachable service give the same answer,
 * empty, because both leave the address step incomplete.
 */
@Component
public class ViaCepClient {

    private static final Logger LOG = LoggerFactory.getLogger(ViaCepClient.class);

    private final RestClient restClient;

    public ViaCepClient(
            RestClient.Builder builder,
            @Value("${viacep.base-url}") String baseUrl,
            @Value("${viacep.timeout:PT3S}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient =
                builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * @param cep eight digits, no hyphen
     * @return the address, or empty when the CEP does not exist or ViaCEP failed
     */
    public Optional<ViaCepAddressDTO> lookup(String cep) {
        try {
            ViaCepAddressDTO address =
                    restClient.get().uri("/{cep}/json/", cep).retrieve().body(ViaCepAddressDTO.class);
            if (address == null || address.uf() == null || address.uf().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(address);
        } catch (RestClientException e) {
            LOG.warn("ViaCEP lookup failed for CEP {}", cep, e);
            return Optional.empty();
        }
    }
}
