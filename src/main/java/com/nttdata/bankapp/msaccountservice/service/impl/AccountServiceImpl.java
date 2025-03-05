package com.nttdata.bankapp.msaccountservice.service.impl;
import com.nttdata.bankapp.msaccountservice.client.CreditCardService;
import com.nttdata.bankapp.msaccountservice.client.CustomerService;
import com.nttdata.bankapp.msaccountservice.dto.AccountDto;
import com.nttdata.bankapp.msaccountservice.dto.BalanceDto;
import com.nttdata.bankapp.msaccountservice.exception.AccountNotFoundException;
import com.nttdata.bankapp.msaccountservice.exception.CustomerNotFoundException;
import com.nttdata.bankapp.msaccountservice.exception.InvalidAccountTypeException;
import com.nttdata.bankapp.msaccountservice.model.Account;
import com.nttdata.bankapp.msaccountservice.model.AccountType;
import com.nttdata.bankapp.msaccountservice.model.CustomerProfile;
import com.nttdata.bankapp.msaccountservice.repository.AccountRepository;
import com.nttdata.bankapp.msaccountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * Implementación de los servicios para operaciones con cuentas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final CreditCardService creditCardService;

    @Override
    public Flux<AccountDto> findAll() {
        log.info("Finding all accounts");
        return accountRepository.findAll()
                .map(this::mapToDto);
    }

    @Override
    public Mono<AccountDto> findById(String id) {
        log.info("Finding account by id: {}", id);
        return accountRepository.findById(id)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)));
    }

    @Override
    public Flux<AccountDto> findByCustomerId(String customerId) {
        log.info("Finding accounts by customer id: {}", customerId);
        return accountRepository.findByCustomerId(customerId)
                .map(this::mapToDto);
    }

    @Override
    public Mono<AccountDto> findByAccountNumber(String accountNumber) {
        log.info("Finding account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with account number: " + accountNumber)));
    }

    @Override
    public Mono<AccountDto> save(AccountDto accountDto) {
        log.info("Saving new account: {}", accountDto);

        // Verificar si el cliente existe y obtener su información
        return customerService.getCustomerDetails(accountDto.getCustomerId())
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + accountDto.getCustomerId())))
                .flatMap(customerDetails -> {
                    AccountDto updatedAccountDto = accountDto.toBuilder()
                            .customerType(customerDetails.getType())
                            .customerProfile(customerDetails.getProfile())
                            .build();

                    return validateAccountRules(updatedAccountDto)
                            .flatMap(valid -> {
                                Account account = mapToEntity(updatedAccountDto);

                                // Generar número de cuenta
                                account.setAccountNumber(generateAccountNumber());

                                // Si no se proporciona monto mínimo de apertura, usar 0
                                if (account.getMinimumOpeningAmount() == null) {
                                    account.setMinimumOpeningAmount(BigDecimal.ZERO);
                                }

                                // Validar monto inicial vs monto mínimo de apertura
                                if (account.getBalance().compareTo(account.getMinimumOpeningAmount()) < 0) {
                                    return Mono.error(new IllegalArgumentException(
                                            "Initial balance must be greater than or equal to minimum opening amount"));
                                }

                                // Establecer valores por defecto según tipo de cuenta y perfil
                                setDefaultValuesBasedOnTypeAndProfile(account);

                                account.setInitialBalance(account.getBalance());
                                account.setCurrentMonthlyTransactions(0);
                                account.setLastTransactionCountResetDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
                                account.setCurrentMonthlyMovements(0);
                                account.setCreatedAt(LocalDateTime.now());
                                account.setUpdatedAt(LocalDateTime.now());

                                return accountRepository.save(account).map(this::mapToDto);
                            });
                });
    }

    @Override
    public Mono<AccountDto> update(String id, AccountDto accountDto) {
        log.info("Updating account id: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)))
                .flatMap(existingAccount -> {
                    // No permitir cambiar campos críticos
                    if (accountDto.getType() != null && !accountDto.getType().equals(existingAccount.getType())) {
                        return Mono.error(new IllegalArgumentException("Cannot change account type"));
                    }

                    if (accountDto.getCustomerId() != null && !accountDto.getCustomerId().equals(existingAccount.getCustomerId())) {
                        return Mono.error(new IllegalArgumentException("Cannot change account owner"));
                    }

                    if (accountDto.getCustomerType() != null && !accountDto.getCustomerType().equals(existingAccount.getCustomerType())) {
                        return Mono.error(new IllegalArgumentException("Cannot change customer type"));
                    }

                    if (accountDto.getCustomerProfile() != null && !accountDto.getCustomerProfile().equals(existingAccount.getCustomerProfile())) {
                        return Mono.error(new IllegalArgumentException("Cannot change customer profile"));
                    }

                    // Actualizar otros campos según sea necesario
                    if (accountDto.getMinimumOpeningAmount() != null) {
                        existingAccount.setMinimumOpeningAmount(accountDto.getMinimumOpeningAmount());
                    }

                    if (accountDto.getMinimumDailyBalance() != null) {
                        existingAccount.setMinimumDailyBalance(accountDto.getMinimumDailyBalance());
                    }

                    if (accountDto.getMaxFreeTransactions() != null) {
                        existingAccount.setMaxFreeTransactions(accountDto.getMaxFreeTransactions());
                    }

                    if (accountDto.getTransactionFee() != null) {
                        existingAccount.setTransactionFee(accountDto.getTransactionFee());
                    }

                    if (accountDto.getMaxMonthlyMovements() != null) {
                        existingAccount.setMaxMonthlyMovements(accountDto.getMaxMonthlyMovements());
                    }

                    if (accountDto.getWithdrawalDay() != null) {
                        existingAccount.setWithdrawalDay(accountDto.getWithdrawalDay());
                    }

                    if (accountDto.getHolders() != null && !accountDto.getHolders().isEmpty()) {
                        existingAccount.setHolders(accountDto.getHolders());
                    }

                    if (accountDto.getSignatories() != null && !accountDto.getSignatories().isEmpty()) {
                        existingAccount.setSignatories(accountDto.getSignatories());
                    }

                    if (accountDto.getMaintenanceFee() != null) {
                        existingAccount.setMaintenanceFee(accountDto.getMaintenanceFee());
                    }

                    existingAccount.setUpdatedAt(LocalDateTime.now());

                    return accountRepository.save(existingAccount);
                })
                .map(this::mapToDto);
    }

    @Override
    public Mono<Void> delete(String id) {
        log.info("Deleting account id: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)))
                .flatMap(account -> accountRepository.deleteById(id));
    }

    @Override
    public Mono<BalanceDto> getBalance(String id) {
        log.info("Getting balance for account id: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)))
                .map(account -> BalanceDto.builder()
                        .accountId(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .accountType(account.getType().toString())
                        .balance(account.getBalance())
                        .remainingMonthlyMovements(account.getType() == AccountType.SAVINGS ?
                                account.getMaxMonthlyMovements() - account.getCurrentMonthlyMovements() : null)
                        .remainingFreeTransactions(account.getMaxFreeTransactions() - account.getCurrentMonthlyTransactions())
                        .transactionFee(account.getTransactionFee())
                        .build());
    }

    @Override
    public Mono<AccountDto> updateBalance(String id, BigDecimal amount) {
        log.info("Updating balance for account id: {} with amount: {}", id, amount);
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)))
                .flatMap(account -> {
                    // Verificar si es un nuevo mes para resetear contadores
                    resetMonthlyCountersIfNeeded(account);

                    BigDecimal newBalance = account.getBalance().add(amount);

                    // Validar que el balance no sea negativo
                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new IllegalArgumentException("Insufficient funds"));
                    }

                    // Actualizar balance
                    account.setBalance(newBalance);

                    // Para depósitos y retiros, incrementar contadores
                    if (amount.compareTo(BigDecimal.ZERO) != 0) {
                        // Incrementar contador de transacciones mensuales
                        account.setCurrentMonthlyTransactions(account.getCurrentMonthlyTransactions() + 1);

                        // Incrementar contador de movimientos mensuales si es una cuenta de ahorro
                        if (account.getType() == AccountType.SAVINGS) {
                            // Verificar si se alcanzó el límite de movimientos
                            if (account.getCurrentMonthlyMovements() >= account.getMaxMonthlyMovements()) {
                                return Mono.error(new IllegalArgumentException("Monthly movements limit reached"));
                            }
                            account.setCurrentMonthlyMovements(account.getCurrentMonthlyMovements() + 1);
                        }

                        // Verificar si es cuenta a plazo fijo
                        if (account.getType() == AccountType.FIXED_TERM && !isWithdrawalDay(account)) {
                            return Mono.error(new IllegalArgumentException("Fixed term accounts can only transact on their withdrawal day"));
                        }
                    }

                    account.setUpdatedAt(LocalDateTime.now());
                    return accountRepository.save(account);
                })
                .map(this::mapToDto);
    }

    @Override
    public Mono<BigDecimal> calculateTransactionFee(String id) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)))
                .map(account -> {
                    // Verificar si es un nuevo mes para resetear contadores
                    resetMonthlyCountersIfNeeded(account);

                    // Si no ha superado las transacciones gratuitas, no hay comisión
                    if (account.getCurrentMonthlyTransactions() < account.getMaxFreeTransactions()) {
                        return BigDecimal.ZERO;
                    }

                    // Si ha superado, cobrar comisión
                    return account.getTransactionFee();
                });
    }

    @Override
    public Mono<AccountDto> incrementTransactionCount(String id, BigDecimal fee) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + id)))
                .flatMap(account -> {
                    // Verificar si es un nuevo mes para resetear contadores
                    resetMonthlyCountersIfNeeded(account);

                    // Incrementar contador de transacciones
                    account.setCurrentMonthlyTransactions(account.getCurrentMonthlyTransactions() + 1);

                    // Si hay comisión, deducirla del saldo
                    if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newBalance = account.getBalance().subtract(fee);

                        // Validar que el balance no sea negativo
                        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                            return Mono.error(new IllegalArgumentException("Insufficient funds for transaction fee"));
                        }

                        account.setBalance(newBalance);
                    }

                    account.setUpdatedAt(LocalDateTime.now());
                    return accountRepository.save(account);
                })
                .map(this::mapToDto);
    }

    @Override
    public Mono<Boolean> validateAccountForTransfer(String accountId, String customerId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found with id: " + accountId)))
                .flatMap(account -> {
                    // Verificar si la cuenta pertenece al cliente
                    if (!account.getCustomerId().equals(customerId)) {
                        return Mono.error(new IllegalArgumentException("Account does not belong to this customer"));
                    }

                    // Verificar si hay fondos suficientes
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new IllegalArgumentException("Insufficient funds"));
                    }

                    // Verificar si es cuenta a plazo fijo (solo puede operar en día específico)
                    if (account.getType() == AccountType.FIXED_TERM && !isWithdrawalDay(account)) {
                        return Mono.error(new IllegalArgumentException("Fixed term accounts can only transact on their withdrawal day"));
                    }

                    return Mono.just(true);
                });
    }

    /**
     * Valida las reglas de negocio para la creación de cuentas.
     * @param accountDto DTO con los datos de la cuenta
     * @return Mono<Boolean> true si es válido, error en caso contrario
     */
    private Mono<Boolean> validateAccountRules(AccountDto accountDto) {
        // Validaciones comunes
        if (accountDto.getBalance() == null) {
            return Mono.error(new IllegalArgumentException("Initial balance is required"));
        }

        // Validar reglas según tipo de cliente y perfil
        switch (accountDto.getCustomerType()) {
            case PERSONAL:
                return validatePersonalAccountRules(accountDto);
            case BUSINESS:
                return validateBusinessAccountRules(accountDto);
            default:
                return Mono.error(new IllegalArgumentException("Invalid customer type"));
        }
    }

    /**
     * Valida reglas para cuentas de clientes personales.
     */
    private Mono<Boolean> validatePersonalAccountRules(AccountDto accountDto) {
        // Cliente personal puede tener máximo una cuenta de cada tipo
        return accountRepository.findByCustomerIdAndType(accountDto.getCustomerId(), accountDto.getType())
                .count()
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.error(new IllegalArgumentException(
                                "Personal customers can only have one account of each type"));
                    }

                    // Para clientes VIP con cuenta de ahorro, verificar que tengan tarjeta de crédito
                    if (accountDto.getCustomerProfile() == CustomerProfile.VIP
                            && accountDto.getType() == AccountType.SAVINGS) {
                        return creditCardService.hasCreditCard(accountDto.getCustomerId())
                                .flatMap(hasCard -> {
                                    if (!hasCard) {
                                        return Mono.error(new IllegalArgumentException(
                                                "VIP customers must have a credit card to open a savings account"));
                                    }
                                    return Mono.just(true);
                                });
                    }

                    return Mono.just(true);
                });
    }

    /**
     * Valida reglas para cuentas de clientes empresariales.
     */
    private Mono<Boolean> validateBusinessAccountRules(AccountDto accountDto) {
        // Cliente empresarial no puede tener cuentas de ahorro o plazo fijo
        if (accountDto.getType() == AccountType.SAVINGS || accountDto.getType() == AccountType.FIXED_TERM) {
            return Mono.error(new InvalidAccountTypeException(
                    "Business customers cannot have savings or fixed term accounts"));
        }

        // Para cuentas empresariales, debe tener al menos un titular
        if (accountDto.getHolders() == null || accountDto.getHolders().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Business accounts must have at least one holder"));
        }

        // Para clientes PYME con cuenta corriente, verificar que tengan tarjeta de crédito
        if (accountDto.getCustomerProfile() == CustomerProfile.PYME
                && accountDto.getType() == AccountType.CHECKING) {
            return creditCardService.hasCreditCard(accountDto.getCustomerId())
                    .flatMap(hasCard -> {
                        if (!hasCard) {
                            return Mono.error(new IllegalArgumentException(
                                    "PYME customers must have a credit card to open a checking account"));
                        }
                        return Mono.just(true);
                    });
        }

        return Mono.just(true);
    }

    /**
     * Establece valores por defecto según el tipo de cuenta y perfil del cliente.
     * @param account Cuenta a configurar
     */
    private void setDefaultValuesBasedOnTypeAndProfile(Account account) {
        // Configuración común para todas las cuentas
        if (account.getMaxFreeTransactions() == null) {
            account.setMaxFreeTransactions(10); // Por defecto 10 transacciones sin comisión
        }

        if (account.getTransactionFee() == null) {
            account.setTransactionFee(new BigDecimal("2.00")); // Por defecto S/2.00 de comisión
        }

        switch (account.getType()) {
            case SAVINGS:
                account.setMaintenanceFee(false);
                if (account.getMaxMonthlyMovements() == null) {
                    account.setMaxMonthlyMovements(20); // Valor por defecto
                }

                // Para clientes VIP
                if (account.getCustomerProfile() == CustomerProfile.VIP) {
                    if (account.getMinimumDailyBalance() == null) {
                        account.setMinimumDailyBalance(new BigDecimal("500.00")); // Mínimo S/500.00 de saldo promedio diario
                    }
                }
                break;

            case CHECKING:
                account.setMaxMonthlyMovements(null); // Sin límite

                // Para clientes regulares o VIP
                if (account.getCustomerProfile() == CustomerProfile.REGULAR
                        || account.getCustomerProfile() == CustomerProfile.VIP) {
                    account.setMaintenanceFee(true);
                }
                // Para clientes PYME
                else if (account.getCustomerProfile() == CustomerProfile.PYME) {
                    account.setMaintenanceFee(false); // Sin comisión de mantenimiento
                }
                break;

            case FIXED_TERM:
                account.setMaintenanceFee(false);
                account.setMaxMonthlyMovements(1);
                if (account.getWithdrawalDay() == null) {
                    // Por defecto, día 1 del mes
                    account.setWithdrawalDay(LocalDate.now().withDayOfMonth(1));
                }
                break;
        }
    }

    /**
     * Verifica si hoy es el día de retiro para una cuenta a plazo fijo.
     * @param account Cuenta a verificar
     * @return true si es el día de retiro, false en caso contrario
     */
    private boolean isWithdrawalDay(Account account) {
        if (account.getType() != AccountType.FIXED_TERM || account.getWithdrawalDay() == null) {
            return true; // No aplica para otros tipos de cuenta
        }

        LocalDate today = LocalDate.now();
        return today.getDayOfMonth() == account.getWithdrawalDay().getDayOfMonth();
    }

    /**
     * Resetea los contadores mensuales si estamos en un nuevo mes.
     * @param account Cuenta a verificar
     */
    private void resetMonthlyCountersIfNeeded(Account account) {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());

        // Si la fecha de último reseteo es de un mes anterior, resetear
        if (account.getLastTransactionCountResetDate() == null ||
                account.getLastTransactionCountResetDate().isBefore(firstDayOfMonth)) {
            account.setCurrentMonthlyTransactions(0);
            account.setCurrentMonthlyMovements(0);
            account.setLastTransactionCountResetDate(firstDayOfMonth);
        }
    }

    /**
     * Genera un número de cuenta aleatorio.
     * @return String con el número de cuenta
     */
    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Convierte una entidad Account a DTO.
     * @param account Entidad a convertir
     * @return AccountDto
     */
    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .type(account.getType())
                .customerId(account.getCustomerId())
                .customerType(account.getCustomerType())
                .customerProfile(account.getCustomerProfile())
                .balance(account.getBalance())
                .initialBalance(account.getInitialBalance())
                .minimumOpeningAmount(account.getMinimumOpeningAmount())
                .minimumDailyBalance(account.getMinimumDailyBalance())
                .maxFreeTransactions(account.getMaxFreeTransactions())
                .transactionFee(account.getTransactionFee())
                .currentMonthlyTransactions(account.getCurrentMonthlyTransactions())
                .maxMonthlyMovements(account.getMaxMonthlyMovements())
                .currentMonthlyMovements(account.getCurrentMonthlyMovements())
                .withdrawalDay(account.getWithdrawalDay())
                .holders(account.getHolders())
                .signatories(account.getSignatories())
                .maintenanceFee(account.getMaintenanceFee())
                .build();
    }

    /**
     * Convierte un DTO a entidad Account.
     * @param accountDto DTO a convertir
     * @return Account
     */
    private Account mapToEntity(AccountDto accountDto) {
        return Account.builder()
                .type(accountDto.getType())
                .customerId(accountDto.getCustomerId())
                .customerType(accountDto.getCustomerType())
                .customerProfile(accountDto.getCustomerProfile())
                .balance(accountDto.getBalance())
                .initialBalance(accountDto.getBalance()) // Inicialmente igual al balance
                .minimumOpeningAmount(accountDto.getMinimumOpeningAmount())
                .minimumDailyBalance(accountDto.getMinimumDailyBalance())
                .maxFreeTransactions(accountDto.getMaxFreeTransactions())
                .transactionFee(accountDto.getTransactionFee())
                .maxMonthlyMovements(accountDto.getMaxMonthlyMovements())
                .withdrawalDay(accountDto.getWithdrawalDay())
                .holders(accountDto.getHolders())
                .signatories(accountDto.getSignatories())
                .maintenanceFee(accountDto.getMaintenanceFee())
                .build();
    }
}