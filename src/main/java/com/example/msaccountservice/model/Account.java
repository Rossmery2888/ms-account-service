package com.example.msaccountservice.model;

import com.example.msaccountservice.model.enums.AccountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;


@Data
@Document(collection = "accounts")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account {
    @Id
    private String id;
    private AccountType accountType;
    private String customerId;
    private List<String> authorizedSigners;
    private BigDecimal balance;
    private BigDecimal maintenanceFee;
    private Integer monthlyTransactionLimit;
    private Integer transactionsPerformed;
    private BigDecimal interestRate;

}
