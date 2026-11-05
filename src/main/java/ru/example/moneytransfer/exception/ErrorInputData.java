package ru.example.moneytransfer.exception;

/**
 * Базовое исключение для ответа 400 (ошибка входных данных).
 */
public class ErrorInputData extends RuntimeException {
    public ErrorInputData(String message) {
        super(message);
    }
}