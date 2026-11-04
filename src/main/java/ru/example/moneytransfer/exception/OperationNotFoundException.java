package ru.example.moneytransfer.exception;

public class OperationNotFoundException extends ErrorInputData {
    public OperationNotFoundException(String message) {
        super(message);
    }
}
