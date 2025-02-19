package com.example.msaccountservice.dto;

import com.example.msaccountservice.model.enums.CustomerProfile;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class SavingsAccountDTO {
    private String id;
    @NotBlank
    private String customerId;
    @NotNull
    private CustomerProfile customerProfile;
    @NotNull
    @Positive
    private BigDecimal balance;
    @NotNull
    private Integer monthlyTransactionLimit;
    private BigDecimal minimumDailyBalance;
    private Boolean hasRequiredCreditCard;
}
