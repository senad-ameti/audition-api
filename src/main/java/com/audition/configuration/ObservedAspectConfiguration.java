package com.audition.configuration;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservedAspectConfiguration {

    @Bean
    public ObservedAspect observedAspect(final ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}