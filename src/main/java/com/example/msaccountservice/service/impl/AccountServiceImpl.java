package com.example.msaccountservice.service.impl;

import com.example.msaccountservice.dto.CheckingAccountDTO;
import com.example.msaccountservice.dto.FixedTermAccountDTO;
import com.example.msaccountservice.dto.SavingsAccountDTO;
import com.example.msaccountservice.model.Account;
import com.example.msaccountservice.model.enums.AccountType;
import com.example.msaccountservice.repository.AccountRepository;
import com.example.msaccountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    @Override
    public Mono<Account> createSavingsAccount(SavingsAccountDTO dto) {
        Account account = new Account();
        account.setAccountType(AccountType.SAVINGS);
        account.setCustomerId(dto.getCustomerId());
        account.setBalance(dto.getBalance());
        account.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());
        account.setTransactionsPerformed(0);
        return accountRepository.save(account);
    }

    @Override
    public Mono<Account> createCheckingAccount(CheckingAccountDTO dto) {
        Account account = new Account();
        account.setAccountType(AccountType.CHECKING);
        account.setCustomerId(dto.getCustomerId());
        account.setBalance(dto.getBalance());
        account.setMaintenanceFee(dto.getMaintenanceFee());
        return accountRepository.save(account);
    }

    @Override
    public Mono<Account> createFixedTermAccount(FixedTermAccountDTO dto) {
        Account account = new Account();
        account.setAccountType(AccountType.FIXED_TERM);
        account.setCustomerId(dto.getCustomerId());
        account.setBalance(dto.getBalance());
        account.setInterestRate(dto.getInterestRate());
        return accountRepository.save(account);
    }

    @Override
    public Flux<Account> getAccountsByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }
    @Override
    public Mono<Account> deposit(String accountId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    account.setBalance(account.getBalance().add(amount));
                    return accountRepository.save(account);
                });
    }

    @Override
    public Mono<Account> withdraw(String accountId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new IllegalArgumentException("Saldo insuficiente"));
                    }
                    // Validar límites y comisiones según el tipo de cuenta
                    if (account.getAccountType() == AccountType.SAVINGS) {
                        if (account.getTransactionsPerformed() >= account.getMonthlyTransactionLimit()) {
                            return Mono.error(new IllegalArgumentException("Límite de transacciones mensuales alcanzado"));
                        }
                        account.setTransactionsPerformed(account.getTransactionsPerformed() + 1);
                    } else if (account.getAccountType() == AccountType.CHECKING && account.getMaintenanceFee() != null) {
                        account.setBalance(account.getBalance().subtract(account.getMaintenanceFee()));
                    }
                    account.setBalance(account.getBalance().subtract(amount));
                    return accountRepository.save(account);
                });
    }

    @Override
    public Mono<Account> updateAuthorizedSigners(String accountId, List<String> authorizedSigners) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    account.setAuthorizedSigners(authorizedSigners);
                    return accountRepository.save(account);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")));
    }
}
