package ru.example.moneytransfer.exception;

public class InsufficientFundsException extends ErrorTransfer {
    public InsufficientFundsException(String message) {
        super(message);
    }
}