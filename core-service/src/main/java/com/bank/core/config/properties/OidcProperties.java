package com.bank.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.oidc")
public class OidcProperties {

    private String issuerUri;
    private String jwkSetUri;
    private String clientId;
    private String authorizationUrl;
    private String tokenUrl;
}
