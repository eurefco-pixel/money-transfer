package ru.example.moneytransfer.exception;

/**
 * Базовое исключение для ответа 500 (ошибка перевода).
 */
public class ErrorTransfer extends RuntimeException {
    public ErrorTransfer(String message) {
        super(message);
    }
}