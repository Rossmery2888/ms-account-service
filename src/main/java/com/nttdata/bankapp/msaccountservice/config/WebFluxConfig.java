package com.nttdata.bankapp.msaccountservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuración de WebFlux.
 */
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
}