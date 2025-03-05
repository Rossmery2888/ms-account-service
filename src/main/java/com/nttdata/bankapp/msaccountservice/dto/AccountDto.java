package com.nttdata.bankapp.msaccountservice.dto;

import com.nttdata.bankapp.msaccountservice.model.AccountType;
import com.nttdata.bankapp.msaccountservice.model.CustomerProfile;
import com.nttdata.bankapp.msaccountservice.model.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para la transferencia de datos de cuentas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountDto {
    private String id;
    private String accountNumber;

    @NotNull(message = "Account type is required")
    private AccountType type;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private CustomerType customerType;
    private CustomerProfile customerProfile;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be non-negative")
    private BigDecimal balance;

    private BigDecimal initialBalance;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum opening amount must be non-negative")
    private BigDecimal minimumOpeningAmount;

    private BigDecimal minimumDailyBalance;

    @Min(value = 0, message = "Max free transactions must be non-negative")
    private Integer maxFreeTransactions;

    @DecimalMin(value = "0.0", inclusive = true, message = "Transaction fee must be non-negative")
    private BigDecimal transactionFee;

    private Integer currentMonthlyTransactions;
    private Integer maxMonthlyMovements;
    private Integer currentMonthlyMovements;
    private LocalDate withdrawalDay;
    private List<String> holders = new ArrayList<>();
    private List<String> signatories = new ArrayList<>();
    private Boolean maintenanceFee;
}
