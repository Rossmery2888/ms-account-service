package com.example.msaccountservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class FixedTermAccountDTO {
    private String id;
    @NotBlank
    private String customerId;
    @NotNull
    @Positive
    private BigDecimal balance;
    @NotNull
    private BigDecimal interestRate;
}