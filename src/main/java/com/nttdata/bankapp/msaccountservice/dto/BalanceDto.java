package com.nttdata.bankapp.msaccountservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la información de saldo de cuenta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceDto {
    private String accountId;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private Integer remainingMonthlyMovements;
    private Integer remainingFreeTransactions;
    private BigDecimal transactionFee;
}