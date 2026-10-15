package br.com.vitor.challengesea.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html. Its Authorize button logs in with email and password through
 * the OAuth2 password flow ({@code POST /auth/token}) and then sends the token on every request.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String LOGIN = "login";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Solicitações de Atendimento API")
                        .contact(new Contact().email("vitorjjhg@hotmail.com").name("Developer: Vitor Costa"))
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                LOGIN,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .description("username is the email; leave client_id and client_secret empty")
                                        .flows(new OAuthFlows()
                                                .password(new OAuthFlow()
                                                        .tokenUrl("/auth/token")
                                                        .scopes(new Scopes())))))
                .addSecurityItem(new SecurityRequirement().addList(LOGIN));
    }
}
