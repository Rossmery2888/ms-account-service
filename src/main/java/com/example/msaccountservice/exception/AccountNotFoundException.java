package com.example.msaccountservice.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String id) {
        super("Account not found with id: " + id);
    }
}