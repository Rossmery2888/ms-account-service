package com.nttdata.bankapp.msaccountservice.exception;

/**
 * Excepción personalizada para tipo de cuenta inválido.
 */
public class InvalidAccountTypeException extends RuntimeException {
    public InvalidAccountTypeException(String message) {
        super(message);
    }
}