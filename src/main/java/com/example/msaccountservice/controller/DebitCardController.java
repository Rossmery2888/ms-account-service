package com.example.msaccountservice.controller;

import com.example.msaccountservice.dto.DebitCardDTO;
import com.example.msaccountservice.dto.DebitCardPaymentDTO;
import com.example.msaccountservice.model.DebitCard;
import com.example.msaccountservice.service.DebitCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/debit-cards")
@RequiredArgsConstructor
public class DebitCardController {

    private final DebitCardService debitCardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DebitCard> createDebitCard(@Valid @RequestBody DebitCardDTO dto) {
        return debitCardService.createDebitCard(dto);
    }

    @PutMapping("/{cardId}/link/{accountId}")
    public Mono<DebitCard> linkAccount(
            @PathVariable String cardId,
            @PathVariable String accountId,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        return debitCardService.linkAccountToDebitCard(cardId, accountId, isPrimary);
    }

    @DeleteMapping("/{cardId}/unlink/{accountId}")
    public Mono<DebitCard> unlinkAccount(
            @PathVariable String cardId,
            @PathVariable String accountId) {
        return debitCardService.unlinkAccountFromDebitCard(cardId, accountId);
    }

    @PostMapping("/payment")
    public Mono<Boolean> processPayment(@Valid @RequestBody DebitCardPaymentDTO dto) {
        return debitCardService.processDebitCardPayment(dto);
    }
}
