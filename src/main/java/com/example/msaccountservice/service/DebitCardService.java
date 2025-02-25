package com.example.msaccountservice.service;

import com.example.msaccountservice.dto.DebitCardDTO;
import com.example.msaccountservice.dto.DebitCardPaymentDTO;
import com.example.msaccountservice.model.DebitCard;
import reactor.core.publisher.Mono;

public interface DebitCardService {
    Mono<DebitCard> createDebitCard(DebitCardDTO dto);
    Mono<DebitCard> linkAccountToDebitCard(String cardId, String accountId, boolean isPrimary);
    Mono<DebitCard> unlinkAccountFromDebitCard(String cardId, String accountId);
    Mono<Boolean> processDebitCardPayment(DebitCardPaymentDTO dto);
}
