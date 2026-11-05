package ru.example.moneytransfer.model;

/**
 * Успешный ответ POST /transfer: { "operationId": "..." }
 */
public record TransferResponse(String operationId) {
}