package ru.example.moneytransfer.model;

/** Единый формат ошибок по спецификации: { "message": "...", "id": число } */
public record ErrorResponse(String message, int id) {
}
