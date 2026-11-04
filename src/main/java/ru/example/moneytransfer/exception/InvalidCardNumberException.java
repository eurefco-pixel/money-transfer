package ru.example.moneytransfer.exception;

public class InvalidCardNumberException extends ErrorInputData {
    public InvalidCardNumberException(String message) {
        super(message);
    }
}