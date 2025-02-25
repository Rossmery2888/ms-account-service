package com.example.msaccountservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "debit_cards")
public class DebitCard {
    @Id
    private String id;
    private String cardNumber;
    private String customerId;
    private String primaryAccountId;
    private List<String> secondaryAccountIds;
}