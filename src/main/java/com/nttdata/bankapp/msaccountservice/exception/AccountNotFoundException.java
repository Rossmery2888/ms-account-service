package com.nttdata.bankapp.msaccountservice.exception;
/**
 * Excepción personalizada para cuenta no encontrada.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
