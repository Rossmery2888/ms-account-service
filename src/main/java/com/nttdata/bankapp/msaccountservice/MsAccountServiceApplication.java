package com.nttdata.bankapp.msaccountservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Clase principal de la aplicación.
 */
@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(info = @Info(
        title = "Account Service API",
        version = "1.0",
        description = "API para la gestión de cuentas bancarias"
))
public class MsAccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAccountServiceApplication.class, args);
    }
}