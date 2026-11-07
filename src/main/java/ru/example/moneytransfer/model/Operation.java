package ru.example.moneytransfer.model;

public record Operation(
        String operationId,
        String cardFromNumber,
        String cardToNumber,
        long amountValue,      // в копейках
        long commission,       // в копейки
        String createdAt,
        boolean confirmed,
        int attempts,          // сделано неверных вводов кода
        String confirmCode     // код, сгенерированный для этой операции (null, если код задан настройкой)
) {
    /*
     Никаких методов, возвращающих Operation.
     Геттер confirmed() уже есть автоматически (возвращает boolean) — этого достаточно.
    */
}