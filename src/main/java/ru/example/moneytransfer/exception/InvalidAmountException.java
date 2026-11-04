package ru.example.moneytransfer.exception;

public class InvalidAmountException extends ErrorInputData {
    public InvalidAmountException(String message) {
        super(message);
    }
}