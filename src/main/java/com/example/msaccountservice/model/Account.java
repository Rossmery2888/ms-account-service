package com.example.msaccountservice.model;

import com.example.msaccountservice.model.enums.AccountType;
import com.example.msaccountservice.model.enums.CustomerProfile;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Data
@Document(collection = "accounts")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account {
    @Id
    private String id;
    private AccountType accountType;
    private CustomerProfile customerProfile;
    private String customerId;
    private List<String> authorizedSigners;
    private BigDecimal balance;
    private BigDecimal maintenanceFee;
    private Integer monthlyTransactionLimit;
    private Integer transactionsPerformed;
    private BigDecimal interestRate;
    private BigDecimal minimumOpeningAmount;
    private BigDecimal minimumDailyBalance;
    private Map<String, BigDecimal> dailyBalances;
    private BigDecimal transactionCommission;
    private Boolean hasRequiredCreditCard;
}
