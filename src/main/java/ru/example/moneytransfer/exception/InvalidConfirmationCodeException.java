package ru.example.moneytransfer.exception;

public class InvalidConfirmationCodeException extends ErrorTransfer {
    public InvalidConfirmationCodeException(String message) {
        super(message);
    }
}
