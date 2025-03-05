package com.nttdata.bankapp.msaccountservice.service;


import com.nttdata.bankapp.msaccountservice.dto.AccountDto;
import com.nttdata.bankapp.msaccountservice.dto.BalanceDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Interfaz que define los servicios para operaciones con cuentas.
 */
public interface AccountService {
    Flux<AccountDto> findAll();
    Mono<AccountDto> findById(String id);
    Flux<AccountDto> findByCustomerId(String customerId);
    Mono<AccountDto> findByAccountNumber(String accountNumber);
    Mono<AccountDto> save(AccountDto accountDto);
    Mono<AccountDto> update(String id, AccountDto accountDto);
    Mono<Void> delete(String id);
    Mono<BalanceDto> getBalance(String id);
    Mono<AccountDto> updateBalance(String id, BigDecimal amount);
    Mono<BigDecimal> calculateTransactionFee(String id);
    Mono<AccountDto> incrementTransactionCount(String id, BigDecimal fee);
    Mono<Boolean> validateAccountForTransfer(String accountId, String customerId, BigDecimal amount);
}