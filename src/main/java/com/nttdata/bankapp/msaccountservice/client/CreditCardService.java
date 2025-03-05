package com.nttdata.bankapp.msaccountservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente para comunicarse con el microservicio de tarjetas de crédito.
 */
@Service
@Slf4j
public class CreditCardService {

    private final WebClient webClient;

    public CreditCardService(@Value("${app.credit-card-service-url}") String creditCardServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(creditCardServiceUrl)
                .build();
    }

    /**
     * Verifica si un cliente tiene una tarjeta de crédito.
     * @param customerId ID del cliente
     * @return Mono<Boolean> true si tiene tarjeta, false en caso contrario
     */
    public Mono<Boolean> hasCreditCard(String customerId) {
        log.info("Checking if customer {} has a credit card", customerId);
        return webClient.get()
                .uri("/credit-cards/customer/{customerId}/exists", customerId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(e -> {
                    log.error("Error checking credit card existence: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}