package com.example.msaccountservice.repository;

import com.example.msaccountservice.model.DebitCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface DebitCardRepository extends ReactiveMongoRepository<DebitCard, String> {
    Mono<DebitCard> findByCardNumber(String cardNumber);
}