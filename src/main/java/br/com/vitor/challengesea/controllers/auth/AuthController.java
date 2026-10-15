package br.com.vitor.challengesea.controllers.auth;

import br.com.vitor.challengesea.model.user.LoginRequestDTO;
import br.com.vitor.challengesea.model.user.OAuthTokenResponseDTO;
import br.com.vitor.challengesea.model.user.RegisterRequestDTO;
import br.com.vitor.challengesea.model.user.TokenResponseDTO;
import br.com.vitor.challengesea.model.user.UserResponseDTO;
import br.com.vitor.challengesea.services.auth.AuthService;
import br.com.vitor.challengesea.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints: client self-registration and login. */
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Client registration and JWT login")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a client", description = "Always creates a CLIENT. 409 when the email is taken.")
    public UserResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = "Returns a bearer JWT for any role. 401 for an unknown email or a wrong password.")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    /**
     * The OAuth2 password grant shape of {@link #login}: form fields in, {@code access_token} out.
     * Swagger UI's Authorize dialog calls it and then sends the token on every request.
     *
     * @param username the email
     * @param password the password
     * @return the same token as {@link #login}
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
            summary = "Log in, OAuth2 form",
            description = "The same login as /auth/login in the OAuth2 password-grant format, for Swagger UI's "
                    + "Authorize button: username is the email.")
    public OAuthTokenResponseDTO token(@RequestParam String username, @RequestParam String password) {
        return OAuthTokenResponseDTO.of(authService.login(new LoginRequestDTO(username, password)));
    }
}
