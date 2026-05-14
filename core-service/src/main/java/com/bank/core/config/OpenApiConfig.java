package com.bank.core.config;

import com.bank.core.config.properties.OidcProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OidcProperties.class)
public class OpenApiConfig {

    private static final String CASDOOR_OAUTH2_SCHEME = "casdoor-oauth2";

    private final OidcProperties oidcProperties;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(CASDOOR_OAUTH2_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(oidcProperties.getAuthorizationUrl())
                                                .tokenUrl(oidcProperties.getTokenUrl())
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect")
                                                        .addString("profile", "Profile")
                                                        .addString("email", "Email"))))))
                .addSecurityItem(new SecurityRequirement().addList(CASDOOR_OAUTH2_SCHEME));
    }
}
