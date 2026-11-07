package ru.example.moneytransfer.dto;

public record ErrorResponse(String message, long id, int code) {
}