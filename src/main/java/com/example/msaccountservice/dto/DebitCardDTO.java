package com.example.msaccountservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class DebitCardDTO {
    private String id;

    @NotBlank
    private String cardNumber;

    @NotBlank
    private String customerId;

    @NotBlank
    private String primaryAccountId;

    @NotEmpty
    private List<String> secondaryAccountIds;
}
