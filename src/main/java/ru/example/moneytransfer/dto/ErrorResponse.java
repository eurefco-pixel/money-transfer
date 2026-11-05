package ru.example.moneytransfer.dto;

public class ErrorResponse {
    private final String message;
    private final int id; // можно использовать как код ошибки внутри приложения

    public ErrorResponse(String message, int id) {
        this.message = message;
        this.id = id;
    }

    // геттеры
    public String getMessage() { return message; }
    public int getId() { return id; }
}