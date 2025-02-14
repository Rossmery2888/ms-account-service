package com.example.msaccountservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDTO {
    private BigDecimal amount;
}
