package br.com.vitor.challengesea.services.auth;

import br.com.vitor.challengesea.config.SecurityConfiguration;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.LoginRequestDTO;
import br.com.vitor.challengesea.model.user.TokenResponseDTO;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exchanges an email and password for a signed JWT. The token carries the user id as
 * {@code sub} and the role; an analyst's state coverage is read from the database on each
 * request instead, so an ADMIN's change applies at once.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.ttl:PT1H}") Duration tokenTtl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
    }

    /**
     * @param request the email and password
     * @return a bearer token valid for {@code security.jwt.ttl}
     * @throws BadCredentialsException when the email is unknown or the password is wrong, without
     *     saying which
     */
    @Transactional(readOnly = true)
    public TokenResponseDTO login(LoginRequestDTO request) {
        AppUser user = userRepository
                .findByEmail(request.email().strip().toLowerCase(Locale.ROOT))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("challenge-sea")
                .subject(user.getId().toString())
                .claim(SecurityConfiguration.ROLE_CLAIM, user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(tokenTtl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponseDTO(token, "Bearer", tokenTtl.toSeconds());
    }
}
