package br.com.vitor.challengesea.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/** {@link TokenResponseDTO} in the OAuth2 token-response shape (RFC 6749), which Swagger UI reads. */
public record OAuthTokenResponseDTO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn) {

    public static OAuthTokenResponseDTO of(TokenResponseDTO token) {
        return new OAuthTokenResponseDTO(token.accessToken(), token.tokenType(), token.expiresIn());
    }
}
