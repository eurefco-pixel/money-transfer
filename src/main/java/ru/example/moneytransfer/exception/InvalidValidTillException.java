package ru.example.moneytransfer.exception;

public class InvalidValidTillException extends RuntimeException {
    public InvalidValidTillException(String message) {
        super(message);
    }
}