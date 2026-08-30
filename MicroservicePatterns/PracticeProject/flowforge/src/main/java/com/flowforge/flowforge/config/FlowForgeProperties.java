package com.flowforge.flowforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "flowforge")
public record FlowForgeProperties(
        @DefaultValue Execution execution,
        @DefaultValue RateLimit rateLimit
) {

    public record Execution(
            @DefaultValue("3") int defaultMaxRetries,
            @DefaultValue("60") int defaultTimeoutSeconds
    ) {}

    public record RateLimit(
            @DefaultValue("50") int maxRequests,
            @DefaultValue("60") int windowSeconds
    ) {}
}
