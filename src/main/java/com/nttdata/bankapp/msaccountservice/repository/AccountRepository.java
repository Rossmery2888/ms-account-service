package com.nttdata.bankapp.msaccountservice.repository;


import com.nttdata.bankapp.msaccountservice.model.Account;
import com.nttdata.bankapp.msaccountservice.model.AccountType;
import com.nttdata.bankapp.msaccountservice.model.CustomerType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio para operaciones CRUD en la colección de cuentas.
 */
@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
    Flux<Account> findByCustomerId(String customerId);
    Mono<Account> findByAccountNumber(String accountNumber);
    Flux<Account> findByCustomerIdAndType(String customerId, AccountType type);
    Flux<Account> findByCustomerIdAndCustomerType(String customerId, CustomerType customerType);
}
