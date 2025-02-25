package com.example.msaccountservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class DebitCardPaymentDTO {
    @NotBlank
    private String cardNumber;

    @NotNull
    @Positive
    private BigDecimal amount;
}