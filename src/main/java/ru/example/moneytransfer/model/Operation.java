package ru.example.moneytransfer.model;

public record Operation(
        String operationId,
        String cardFromNumber,
        String cardToNumber,
        long amountValue,      // в копейках
        long commission,       // в копейках
        String createdAt,
        boolean confirmed
) {
    // Никаких методов, возвращающих Operation.
    // Геттер confirmed() уже есть автоматически (возвращает boolean) — этого достаточно.
}