package com.example.msaccountservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class TransferDTO {
    @NotBlank
    private String sourceAccountId;
    @NotBlank
    private String destinationAccountId;
    @NotNull
    @Positive
    private BigDecimal amount;
}