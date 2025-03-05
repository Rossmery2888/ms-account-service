package com.nttdata.bankapp.msaccountservice.client;

import com.nttdata.bankapp.msaccountservice.model.CustomerProfile;
import com.nttdata.bankapp.msaccountservice.model.CustomerType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


/**
 * Cliente para comunicarse con el microservicio de clientes.
 */
@Slf4j
@Service
public class CustomerService {

    private final WebClient webClient;

    public CustomerService(@Value("${app.customer-service-url}") String customerServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(customerServiceUrl)
                .build();
    }

    /**
     * Verifica si un cliente existe por su ID.
     * @param customerId ID del cliente
     * @return Mono<Boolean> true si existe, false en caso contrario
     */
    public Mono<Boolean> customerExists(String customerId) {
        log.info("Checking if customer exists with id: {}", customerId);
        return webClient.get()
                .uri("/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(response -> true)
                .onErrorResume(e -> {
                    log.error("Error checking customer existence: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Obtiene la información detallada de un cliente.
     * @param customerId ID del cliente
     * @return Mono<CustomerDetails>
     */
    public Mono<CustomerDetails> getCustomerDetails(String customerId) {
        log.info("Getting customer details for id: {}", customerId);
        return webClient.get()
                .uri("/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(CustomerDetails.class)
                .onErrorResume(e -> {
                    log.error("Error getting customer details: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @lombok.Data
    public static class CustomerDetails {
        private String id;
        private String documentNumber;
        private CustomerType type;
        private CustomerProfile profile;
        private String firstName;
        private String lastName;
        private String businessName;
    }
    // DTO interno para mapear la respuesta del servicio de clientes
    @Data
    private static class CustomerDto {
        private String id;
        private CustomerTypeEnum type;
    }

    private enum CustomerTypeEnum {
        PERSONAL, BUSINESS
    }
}