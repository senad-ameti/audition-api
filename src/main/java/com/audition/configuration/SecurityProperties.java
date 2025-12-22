package com.audition.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for security settings. Binds to properties under the 'audition.security' prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "audition.security")
@Getter
@Setter
public class SecurityProperties {

    /**
     * API key required to access secured actuator endpoints. Should be provided via ACTUATOR_API_KEY environment
     * variable.
     */
    private String actuatorApiKey;
}

