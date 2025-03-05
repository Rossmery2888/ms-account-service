package com.nttdata.bankapp.msaccountservice.controller;


import com.nttdata.bankapp.msaccountservice.dto.AccountDto;
import com.nttdata.bankapp.msaccountservice.dto.BalanceDto;
import com.nttdata.bankapp.msaccountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final AccountService accountService;

    /**
     * Obtiene todas las cuentas.
     * @return Flux de AccountDto
     */
    @GetMapping
    public Flux<AccountDto> getAll() {
        log.info("GET /accounts");
        return accountService.findAll();
    }

    /**
     * Obtiene una cuenta por su ID.
     * @param id ID de la cuenta
     * @return Mono de AccountDto
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AccountDto>> getById(@PathVariable String id) {
        log.info("GET /accounts/{}", id);
        return accountService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    /**
     * Obtiene las cuentas de un cliente.
     * @param customerId ID del cliente
     * @return Flux de AccountDto
     */
    @GetMapping("/customer/{customerId}")
    public Flux<AccountDto> getByCustomerId(@PathVariable String customerId) {
        log.info("GET /accounts/customer/{}", customerId);
        return accountService.findByCustomerId(customerId);
    }

    /**
     * Obtiene una cuenta por su número.
     * @param accountNumber Número de cuenta
     * @return Mono de AccountDto
     */
    @GetMapping("/number/{accountNumber}")
    public Mono<ResponseEntity<AccountDto>> getByAccountNumber(@PathVariable String accountNumber) {
        log.info("GET /accounts/number/{}", accountNumber);
        return accountService.findByAccountNumber(accountNumber)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Consulta el saldo de una cuenta.
     * @param id ID de la cuenta
     * @return Mono de BalanceDto
     */
    @GetMapping("/{id}/balance")
    public Mono<ResponseEntity<BalanceDto>> getBalance(@PathVariable String id) {
        log.info("GET /accounts/{}/balance", id);
        return accountService.getBalance(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva cuenta.
     * @param accountDto DTO con los datos de la cuenta
     * @return Mono de AccountDto
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AccountDto> create(@Valid @RequestBody AccountDto accountDto) {
        log.info("POST /accounts");
        return accountService.save(accountDto);
    }

    /**
     * Actualiza una cuenta existente.
     * @param id ID de la cuenta
     * @param accountDto DTO con los datos a actualizar
     * @return Mono de AccountDto
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<AccountDto>> update(@PathVariable String id, @Valid @RequestBody AccountDto accountDto) {
        log.info("PUT /accounts/{}", id);
        return accountService.update(id, accountDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una cuenta.
     * @param id ID de la cuenta
     * @return Mono<Void>
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        log.info("DELETE /accounts/{}", id);
        return accountService.delete(id);
    }

    /**
     * Endpoint para actualizar el saldo de una cuenta (usado internamente por el servicio de transacciones).
     * @param id ID de la cuenta
     * @param amount Monto a actualizar (positivo para depósitos, negativo para retiros)
     * @return Mono de AccountDto
     */
    @PutMapping("/{id}/balance")
    public Mono<ResponseEntity<AccountDto>> updateBalance(
            @PathVariable String id,
            @RequestParam BigDecimal amount) {
        log.info("PUT /accounts/{}/balance with amount: {}", id, amount);
        return accountService.updateBalance(id, amount)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    /**
     * Valida una cuenta para transferencia.
     * @param id ID de la cuenta
     * @param customerId ID del cliente
     * @param amount Monto a transferir
     * @return Mono<Boolean>
     */
    @GetMapping("/{id}/validate-transfer")
    public Mono<Boolean> validateForTransfer(
            @PathVariable String id,
            @RequestParam String customerId,
            @RequestParam BigDecimal amount) {
        log.info("GET /accounts/{}/validate-transfer with customerId: {} and amount: {}", id, customerId, amount);
        return accountService.validateAccountForTransfer(id, customerId, amount);
    }

    /**
     * Obtiene la comisión por transacción para una cuenta.
     * @param id ID de la cuenta
     * @return Mono<BigDecimal>
     */
    @GetMapping("/{id}/transaction-fee")
    public Mono<BigDecimal> getTransactionFee(@PathVariable String id) {
        log.info("GET /accounts/{}/transaction-fee", id);
        return accountService.calculateTransactionFee(id);
    }

    /**
     * Incrementa el contador de transacciones y aplica comisión si es necesario.
     * @param id ID de la cuenta
     * @param fee Comisión a aplicar (puede ser 0)
     * @return Mono<AccountDto>
     */
    @PutMapping("/{id}/transaction-count")
    public Mono<AccountDto> incrementTransactionCount(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "0") BigDecimal fee) {
        log.info("PUT /accounts/{}/transaction-count with fee: {}", id, fee);
        BigDecimal feeToApply = fee.compareTo(BigDecimal.ZERO) > 0 ? fee : null;
        return accountService.incrementTransactionCount(id, feeToApply);
    }
}