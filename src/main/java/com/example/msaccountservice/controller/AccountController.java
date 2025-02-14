package com.example.msaccountservice.controller;


import com.example.msaccountservice.dto.CheckingAccountDTO;
import com.example.msaccountservice.dto.FixedTermAccountDTO;
import com.example.msaccountservice.dto.SavingsAccountDTO;
import com.example.msaccountservice.dto.TransactionDTO;
import com.example.msaccountservice.model.Account;
import com.example.msaccountservice.service.AccountService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/savings")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Account> createSavingsAccount(@Valid @RequestBody SavingsAccountDTO dto) {
        return accountService.createSavingsAccount(dto);
    }

    @PostMapping("/checking")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Account> createCheckingAccount(@Valid @RequestBody CheckingAccountDTO dto) {
        return accountService.createCheckingAccount(dto);
    }

    @PostMapping("/fixed-term")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Account> createFixedTermAccount(@Valid @RequestBody FixedTermAccountDTO dto) {
        return accountService.createFixedTermAccount(dto);
    }

    @GetMapping("/customer/{customerId}")
    public Flux<Account> getAccountsByCustomerId(@PathVariable String customerId) {
        return accountService.getAccountsByCustomerId(customerId);
    }

    @PostMapping("/{accountId}/deposit")
    public Mono<Account> deposit(@PathVariable String accountId, @RequestBody TransactionDTO transactionDTO) {
        return accountService.deposit(accountId, transactionDTO.getAmount());
    }

    @PostMapping("/{accountId}/withdraw")
    public Mono<Account> withdraw(@PathVariable String accountId, @RequestBody TransactionDTO transactionDTO) {
        return accountService.withdraw(accountId, transactionDTO.getAmount());
    }

    @PutMapping("/{accountId}/signers")
    public Mono<Account> updateAuthorizedSigners(
            @PathVariable String accountId,
            @RequestBody SignersRequest request) {
        return accountService.updateAuthorizedSigners(accountId, request.getAuthorizedSigners());
    }

    @Data
    public static class SignersRequest {
        private List<String> authorizedSigners;
    }

}