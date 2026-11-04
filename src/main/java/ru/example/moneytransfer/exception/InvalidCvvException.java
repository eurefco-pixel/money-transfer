package ru.example.moneytransfer.exception;

public class InvalidCvvException extends ErrorInputData {
    public InvalidCvvException(String message) {
        super(message);
    }
}