package com.nttdata.bankapp.msaccountservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo que representa una cuenta bancaria.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;

    @Indexed(unique = true)
    private String accountNumber;
    private AccountType type; // SAVINGS, CHECKING, FIXED_TERM
    private String customerId;
    private CustomerType customerType; // PERSONAL, BUSINESS
    private CustomerProfile customerProfile; // REGULAR, VIP, PYME
    private BigDecimal initialBalance; // Saldo inicial al crear la cuenta
    private BigDecimal balance;
    private BigDecimal minimumOpeningAmount; // Monto mínimo de apertura
    private BigDecimal minimumDailyBalance; // Saldo promedio diario mínimo requerido (para VIP)
    private Integer maxFreeTransactions; // Máximo de transacciones sin comisión
    private BigDecimal transactionFee; // Comisión por transacción después del límite
    private Integer currentMonthlyTransactions; // Contador de transacciones del mes actual
    private LocalDate lastTransactionCountResetDate; // Fecha del último reseteo del contador
    private Integer maxMonthlyMovements; // Solo para cuenta de ahorro
    private Integer currentMonthlyMovements;
    private LocalDate withdrawalDay; // Solo para cuenta a plazo fijo
    private List<String> holders = new ArrayList<>(); // Titulares (solo para cuentas empresariales)
    private List<String> signatories = new ArrayList<>(); // Firmantes (solo para cuentas empresariales)
    private Boolean maintenanceFee; // Comisión de mantenimiento
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
