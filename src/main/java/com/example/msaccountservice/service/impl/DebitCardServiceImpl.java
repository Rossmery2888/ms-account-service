package com.example.msaccountservice.service.impl;

import com.example.msaccountservice.dto.DebitCardDTO;
import com.example.msaccountservice.dto.DebitCardPaymentDTO;
import com.example.msaccountservice.exception.AccountNotFoundException;
import com.example.msaccountservice.exception.BusinessValidationException;
import com.example.msaccountservice.model.Account;
import com.example.msaccountservice.model.DebitCard;
import com.example.msaccountservice.repository.AccountRepository;
import com.example.msaccountservice.repository.DebitCardRepository;
import com.example.msaccountservice.service.DebitCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitCardServiceImpl implements DebitCardService {

    private final DebitCardRepository debitCardRepository;
    private final AccountRepository accountRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    @Override
    public Mono<DebitCard> createDebitCard(DebitCardDTO dto) {
        return validateDebitCardCreation(dto)
                .flatMap(valid -> {
                    DebitCard debitCard = new DebitCard();
                    debitCard.setCardNumber(dto.getCardNumber());
                    debitCard.setCustomerId(dto.getCustomerId());
                    debitCard.setPrimaryAccountId(dto.getPrimaryAccountId());
                    debitCard.setSecondaryAccountIds(dto.getSecondaryAccountIds());
                    return debitCardRepository.save(debitCard);
                });
    }

    private Mono<Boolean> validateDebitCardCreation(DebitCardDTO dto) {
        return accountRepository.findById(dto.getPrimaryAccountId())
                .switchIfEmpty(Mono.error(new AccountNotFoundException(dto.getPrimaryAccountId())))
                .flatMap(primaryAccount -> {
                    if (!primaryAccount.getCustomerId().equals(dto.getCustomerId())) {
                        return Mono.error(new BusinessValidationException(
                                "Primary account must belong to the customer"));
                    }

                    List<Mono<Account>> secondaryAccountMonos = dto.getSecondaryAccountIds().stream()
                            .map(accountId -> accountRepository.findById(accountId)
                                    .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId))))
                            .collect(Collectors.toList());

                    return Mono.zip(secondaryAccountMonos, accounts -> accounts)
                            .flatMap(accounts -> {
                                for (Object accountObj : accounts) {
                                    Account account = (Account) accountObj;
                                    if (!account.getCustomerId().equals(dto.getCustomerId())) {
                                        return Mono.error(new BusinessValidationException(
                                                "Secondary account " + account.getId() + " must belong to the customer"));
                                    }
                                }
                                return Mono.just(true);
                            });
                });
    }

    @Override
    public Mono<DebitCard> linkAccountToDebitCard(String cardId, String accountId, boolean isPrimary) {
        return debitCardRepository.findById(cardId)
                .switchIfEmpty(Mono.error(new BusinessValidationException("Debit card not found")))
                .flatMap(debitCard -> accountRepository.findById(accountId)
                        .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)))
                        .flatMap(account -> {
                            if (!account.getCustomerId().equals(debitCard.getCustomerId())) {
                                return Mono.error(new BusinessValidationException("Account must belong to the same customer"));
                            }

                            if (isPrimary) {
                                debitCard.setPrimaryAccountId(accountId);
                            } else if (!debitCard.getSecondaryAccountIds().contains(accountId)) {
                                debitCard.getSecondaryAccountIds().add(accountId);
                            }

                            return debitCardRepository.save(debitCard);
                        }));
    }

    @Override
    public Mono<DebitCard> unlinkAccountFromDebitCard(String cardId, String accountId) {
        return debitCardRepository.findById(cardId)
                .switchIfEmpty(Mono.error(new BusinessValidationException("Debit card not found")))
                .flatMap(debitCard -> {
                    if (debitCard.getPrimaryAccountId().equals(accountId)) {
                        return Mono.error(new BusinessValidationException("Cannot unlink primary account. Link a new primary account first."));
                    }

                    if (debitCard.getSecondaryAccountIds().contains(accountId)) {
                        debitCard.setSecondaryAccountIds(
                                debitCard.getSecondaryAccountIds().stream()
                                        .filter(id -> !id.equals(accountId))
                                        .collect(Collectors.toList())
                        );
                        return debitCardRepository.save(debitCard);
                    }

                    return Mono.just(debitCard);
                });
    }

    @Override
    public Mono<Boolean> processDebitCardPayment(DebitCardPaymentDTO dto) {
        return debitCardRepository.findByCardNumber(dto.getCardNumber())
                .switchIfEmpty(Mono.error(new BusinessValidationException("Debit card not found")))
                .flatMap(debitCard -> {
                    // Check primary account first
                    return processPaymentWithAccount(debitCard.getPrimaryAccountId(), dto.getAmount())
                            .onErrorResume(e -> {
                                log.info("Payment from primary account failed: {}", e.getMessage());
                                // If primary account fails, try secondary accounts in order
                                List<String> accountIds = new ArrayList<>(debitCard.getSecondaryAccountIds());
                                return trySecondaryAccounts(accountIds, dto.getAmount(), 0);
                            });
                });
    }

    private Mono<Boolean> trySecondaryAccounts(List<String> accountIds, BigDecimal amount, int index) {
        if (index >= accountIds.size()) {
            return Mono.error(new BusinessValidationException("Insufficient funds in all linked accounts"));
        }

        String accountId = accountIds.get(index);
        return processPaymentWithAccount(accountId, amount)
                .onErrorResume(e -> {
                    log.info("Payment from secondary account {} failed: {}", accountId, e.getMessage());
                    return trySecondaryAccounts(accountIds, amount, index + 1);
                });
    }

    private Mono<Boolean> processPaymentWithAccount(String accountId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)))
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new BusinessValidationException("Insufficient funds in account " + accountId));
                    }

                    // Initialize dailyBalances if it is null
                    if (account.getDailyBalances() == null) {
                        account.setDailyBalances(new HashMap<>());
                    }

                    // Deduct the amount
                    account.setBalance(account.getBalance().subtract(amount));

                    // Update daily balance with formatted date
                    account.getDailyBalances().put(formatDateTime(LocalDateTime.now()), account.getBalance());

                    // Increment transaction count if applicable
                    if (account.getTransactionsPerformed() != null) {
                        account.setTransactionsPerformed(account.getTransactionsPerformed() + 1);
                    }

                    return accountRepository.save(account).map(a -> true);
                });
    }
}