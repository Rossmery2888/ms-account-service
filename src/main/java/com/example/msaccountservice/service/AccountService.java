package com.example.msaccountservice.service;


import com.example.msaccountservice.dto.CheckingAccountDTO;
import com.example.msaccountservice.dto.FixedTermAccountDTO;
import com.example.msaccountservice.dto.SavingsAccountDTO;
import com.example.msaccountservice.dto.TransferDTO;
import com.example.msaccountservice.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AccountService {
    Mono<Account> createSavingsAccount(SavingsAccountDTO dto);
    Mono<Account> createCheckingAccount(CheckingAccountDTO dto);
    Mono<Account> createFixedTermAccount(FixedTermAccountDTO dto);
    Flux<Account> getAccountsByCustomerId(String customerId);
    Mono<Account> deposit(String accountId, BigDecimal amount);
    Mono<Account> withdraw(String accountId, BigDecimal amount);
    Mono<Account> updateAuthorizedSigners(String accountId, List<String> authorizedSigners);
    Mono<Account> transfer(TransferDTO transferDTO);
    Mono<Map<String, BigDecimal>> getAverageBalanceReport(String customerId);
    Mono<Map<String, BigDecimal>> getCommissionsReport(String customerId, LocalDateTime startDate, LocalDateTime endDate);

}