package ru.example.moneytransfer.exception;

public class CardNotFoundException extends ErrorTransfer {
    public CardNotFoundException(String message) {
        super(message);
    }
}