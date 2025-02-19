package com.example.msaccountservice.service.impl;

import com.example.msaccountservice.dto.CheckingAccountDTO;
import com.example.msaccountservice.dto.FixedTermAccountDTO;
import com.example.msaccountservice.dto.SavingsAccountDTO;
import com.example.msaccountservice.dto.TransferDTO;
import com.example.msaccountservice.exception.BusinessValidationException;
import com.example.msaccountservice.model.Account;
import com.example.msaccountservice.model.enums.AccountType;
import com.example.msaccountservice.model.enums.CustomerProfile;
import com.example.msaccountservice.repository.AccountRepository;
import com.example.msaccountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    private static final BigDecimal VIP_MINIMUM_DAILY_BALANCE = new BigDecimal("1000");
    private static final BigDecimal DEFAULT_TRANSACTION_COMMISSION = new BigDecimal("1.0");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }
    @Override
    public Mono<Account> createSavingsAccount(SavingsAccountDTO dto) {
        return validateAccountCreation(dto)
                .flatMap(valid -> {
                    Account account = new Account();
                    account.setAccountType(AccountType.SAVINGS);
                    account.setCustomerId(dto.getCustomerId());
                    account.setCustomerProfile(dto.getCustomerProfile());
                    account.setBalance(dto.getBalance());
                    account.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());
                    account.setTransactionsPerformed(0);
                    account.setTransactionCommission(DEFAULT_TRANSACTION_COMMISSION);

                    // Inicializar el mapa con la fecha formateada como String
                    account.setDailyBalances(new HashMap<>());
                    account.getDailyBalances().put(formatDateTime(LocalDateTime.now()), dto.getBalance());

                    if (CustomerProfile.VIP.equals(dto.getCustomerProfile())) {
                        account.setMinimumDailyBalance(VIP_MINIMUM_DAILY_BALANCE);
                        account.setHasRequiredCreditCard(dto.getHasRequiredCreditCard());
                    }

                    return accountRepository.save(account);
                });
    }

    private Mono<Boolean> validateAccountCreation(SavingsAccountDTO dto) {
        if (CustomerProfile.VIP.equals(dto.getCustomerProfile())) {
            if (!dto.getHasRequiredCreditCard()) {
                return Mono.error(new BusinessValidationException("VIP accounts require an active credit card"));
            }
            if (dto.getBalance().compareTo(VIP_MINIMUM_DAILY_BALANCE) < 0) {
                return Mono.error(new BusinessValidationException("VIP accounts require a minimum balance of " + VIP_MINIMUM_DAILY_BALANCE));
            }
        }
        return Mono.just(true);
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
        account.setDailyBalances(new HashMap<>()); // Inicializar el map
        account.getDailyBalances().put(formatDateTime(LocalDateTime.now()), dto.getBalance()); // Registrar saldo inicial
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
                    if (account.getDailyBalances() == null) {
                        account.setDailyBalances(new HashMap<>());
                    }

                    account.setBalance(account.getBalance().add(amount));

                    // Actualizar saldo diario con la fecha formateada
                    account.getDailyBalances().put(formatDateTime(LocalDateTime.now()), account.getBalance());

                    return accountRepository.save(account);
                });
    }

    @Override
    public Mono<Account> withdraw(String accountId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new BusinessValidationException("Insufficient funds"));
                    }

                    // Inicializar dailyBalances si es null
                    if (account.getDailyBalances() == null) {
                        account.setDailyBalances(new HashMap<>());
                    }

                    BigDecimal commission = BigDecimal.ZERO;
                    if (account.getTransactionsPerformed() >= account.getMonthlyTransactionLimit()
                            && !CustomerProfile.PYME.equals(account.getCustomerProfile())) {
                        commission = account.getTransactionCommission();
                    }

                    account.setBalance(account.getBalance().subtract(amount).subtract(commission));
                    account.setTransactionsPerformed(
                            account.getTransactionsPerformed() != null ?
                                    account.getTransactionsPerformed() + 1 : 1
                    );

                    // Actualizar saldo diario con la fecha formateada
                    account.getDailyBalances().put(formatDateTime(LocalDateTime.now()), account.getBalance());

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

    @Override
    public Mono<Account> transfer(TransferDTO transferDTO) {
        return accountRepository.findById(transferDTO.getSourceAccountId())
                .flatMap(sourceAccount ->
                        accountRepository.findById(transferDTO.getDestinationAccountId())
                                .flatMap(destinationAccount -> {
                                    return withdraw(sourceAccount.getId(), transferDTO.getAmount())
                                            .flatMap(updatedSource ->
                                                    deposit(destinationAccount.getId(), transferDTO.getAmount()));
                                })
                );
    }
    @Override
    public Mono<Map<String, BigDecimal>> getCommissionsReport(String customerId, LocalDateTime startDate, LocalDateTime endDate) {
        return accountRepository.findByCustomerId(customerId)
                .collectList()
                .map(accounts -> {
                    Map<String, BigDecimal> commissions = new HashMap<>();
                    accounts.forEach(account -> {
                        if (!CustomerProfile.PYME.equals(account.getCustomerProfile())) {
                            int excessTransactions = Math.max(0, account.getTransactionsPerformed() - account.getMonthlyTransactionLimit());
                            BigDecimal totalCommission = account.getTransactionCommission()
                                    .multiply(new BigDecimal(excessTransactions));
                            commissions.put(account.getId(), totalCommission);
                        }
                    });
                    return commissions;
                });
    }
    @Override
    public Mono<Map<String, BigDecimal>> getAverageBalanceReport(String customerId) {
        return accountRepository.findByCustomerId(customerId)
                .collectList()
                .map(accounts -> {
                    Map<String, BigDecimal> averages = new HashMap<>();
                    accounts.forEach(account -> {
                        if (account.getDailyBalances() != null && !account.getDailyBalances().isEmpty()) {
                            BigDecimal avgBalance = account.getDailyBalances().values().stream()
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(new BigDecimal(account.getDailyBalances().size()), 2, RoundingMode.HALF_UP);
                            averages.put(account.getId(), avgBalance);
                        } else {
                            averages.put(account.getId(), account.getBalance());
                        }
                    });
                    return averages;
                });
    }

}
